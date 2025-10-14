/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package tri.ai.core.agent.impl

import com.fasterxml.jackson.databind.node.ArrayNode
import kotlinx.coroutines.flow.FlowCollector
import tri.ai.core.MChatRole
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.core.agent.AgentChatEvent
import tri.ai.core.agent.AgentChatResponse
import tri.ai.core.agent.AgentChatSession
import tri.ai.core.agent.AgentToolChatSupport
import tri.ai.core.agent.MAPPER
import tri.ai.core.agent.createObject
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.Executable
import tri.ai.core.tool.ToolExecutableResult
import tri.ai.prompt.fill

/**
 * Executes a user prompt using a set of tools using a planning operation.
 * Uses a [TextChat] to complete a Question/Thought/Action/Action Input/Observation/.../Thought/Final Answer loop.
 * Tools should support getting input of the form {"input":"xxx"}, e.g. [tri.ai.core.tool.ToolExecutable].
 * Depending on the model, this approach may frequently repeat or fail to terminate.
 */
class ToolChainExecutor(tools: List<Executable>) : AgentToolChatSupport(tools) {

    companion object {
        private val TOOL_CHAIN_PROMPT = PROMPTS.get("tools/chain-executor")!!
    }

    val iterationLimit = 5
    val completionTokens = 2000

    private fun createScratchpad() = ExecContext().apply { vars["steps"] = MAPPER.createArrayNode() }
    private fun ExecContext.steps() = vars["steps"] as ArrayNode
    private fun ExecContext.summary() = steps().values().asSequence().joinToString("\n") { it.asText() }

    override suspend fun FlowCollector<AgentChatEvent>.sendMessageSafe(session: AgentChatSession, message: MultimodalChatMessage): AgentChatResponse {
        val question = logTextContent(message)
        updateSession(message, session)
        logToolUsage()

        // prepare chat and scratchpad
        val chat = findChat(session, this)
        val scratchpad = createScratchpad()

        // run execution chain until we get a final answer or hit the iteration limit
        var result: ToolExecutableResult? = null
        var iterations = 0
        while (result?.isTerminal != true && iterations++ < iterationLimit) {
            result = runExecChain(chat, templateForQuestion(question), scratchpad)
        }
        val final = result?.result ?: "I was unable to determine a final answer."
        val response = MultimodalChatMessage.text(MChatRole.Assistant, final)

        // store and log response, maybe update session name
        updateSession(response, session, updateName = true)
        return agentChatResponse(response, session)
    }

    private fun templateForQuestion(question: String) = TOOL_CHAIN_PROMPT.fill(
        "tools" to tools.joinToString("\n") { "${it.name}: ${it.description}" },
        "tool_names" to tools.joinToString(", ") { it.name },
        "input" to question,
        "agent_scratchpad" to "{{agent_scratchpad}}" // must be this exact string for replacement later
    )

    /** Runs a single step of an execution chain. */
    private suspend fun FlowCollector<AgentChatEvent>.runExecChain(chat: TextChat, promptTemplate: String, scratchpad: ExecContext): ToolExecutableResult {
        val prompt = promptTemplate.replace("{{agent_scratchpad}}", scratchpad.summary())

        val completion = chat.chat(listOf(TextChatMessage.user(prompt)), stop = listOf("Observation: "), tokens = completionTokens)
            .firstValue.textContent().trim()
            .replace("\n\n", "\n")
        emit(AgentChatEvent.Reasoning(completion))
        scratchpad.steps().add(completion)

        if ("Final Answer:" in completion) {
            val answer = completion.substringAfter("Final Answer:").trim()
            return ToolExecutableResult(answer, true)
        }

        val responseOp = completion.parseKeyValuePairs()
        val tool = tools.find { it.name == responseOp["Action"] }
        if (tool == null) {
            val fallback = "Invalid or missing tool name."
            emit(AgentChatEvent.Error(IllegalArgumentException(fallback)))
            scratchpad.steps().add("Observation: $fallback")
            return ToolExecutableResult(fallback, false)
        }
        val toolInput = responseOp["Action Input"]
            ?.ifBlank { completion.substringAfter("Action Input:").trim() }
            ?.ifBlank { null }
        if (toolInput.isNullOrBlank()) {
            val fallback ="Tool input missing or malformed."
            emit(AgentChatEvent.Error(IllegalArgumentException(fallback)))
            scratchpad.steps().add("Observation: $fallback")
            return ToolExecutableResult(fallback, false)
        }

        // execute tool
        val inputJson = createObject("input", toolInput)
        emit(AgentChatEvent.UsingTool(tool.name, toolInput))
        val executionResult = tool.execute(inputJson, ExecContext())

        // log and return result
        val resultText = executionResult.get("result")?.asText() ?: ""
        emit(AgentChatEvent.ToolResult(tool.name, resultText))
        scratchpad.steps().add("Observation: $resultText")

        return ToolExecutableResult(
            resultText,
            isTerminal = executionResult.get("isTerminal")?.asBoolean() ?: false,
        )
    }

    private fun String.parseKeyValuePairs() = split("\n")
        .map { it.split(":", limit = 2) }
        .filter { it.size == 2 }
        .associate { it[0].trim() to it[1].trim() }

}
