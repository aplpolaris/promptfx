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
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.Executable
import tri.ai.core.tool.ToolExecutableResult
import tri.ai.prompt.fill
import tri.util.*

/**
 * Executes a user prompt using a set of tools using a planning operation.
 * Uses a [tri.ai.core.TextCompletion] to complete a Question/Thought/Action/Action Input/Observation/.../Thought/Final Answer loop.
 * Depending on the model, this approach may frequently repeat or fail to terminate.
 */
class ToolChainExecutor(tools: List<Executable>) : AgentToolChatSupport(tools) {

    companion object {
        private val TOOL_CHAIN_PROMPT = PROMPTS.get("tools/chain-executor")!!
    }

    val iterationLimit = 5
    val completionTokens = 500

    override suspend fun FlowCollector<AgentChatEvent>.sendMessageSafe(session: AgentChatSession, message: MultimodalChatMessage): AgentChatResponse {
        val question = logTextContent(message)
        updateSession(message, session)
        logToolUsage()

        // prepare chat and scratchpad
        val chat = findChat(session, this)
        val scratchpad = ExecContext().apply {
            vars["steps"] = MAPPER.createArrayNode()
        }
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

    fun ExecContext.summary() = (vars["steps"] as ArrayNode).values().asSequence().joinToString("\n") { it.asText() }

    /** Runs a single step of an execution chain. */
    private suspend fun FlowCollector<AgentChatEvent>.runExecChain(chat: TextChat, promptTemplate: String, scratchpad: ExecContext): ToolExecutableResult {
        val prompt = promptTemplate.replace("{{agent_scratchpad}}", scratchpad.summary())

        val completion = chat.chat(listOf(TextChatMessage.user(prompt)), stop = listOf("Observation: "), tokens = completionTokens)
            .firstValue.textContent().trim()
            .replace("\n\n", "\n")
        emit(AgentChatEvent.Reasoning(completion))
        (scratchpad.vars["steps"] as ArrayNode).add(completion)

        val responseOp = completion.split("\n")
            .map { it.split(":", limit = 2) }
            .filter { it.size == 2 }
            .associate { it[0].trim() to it[1].trim() }
        if ("Final Answer:" in completion) {
            val answer = completion.substringAfter("Final Answer:").trim()
            return ToolExecutableResult(answer, true)
        }

        val toolName = responseOp["Action"]?.trim()
        val tool = tools.find { it.name == toolName }
        if (tool == null) {
            val fallback = "Invalid or missing tool name."
            emit(AgentChatEvent.Error(IllegalArgumentException(fallback)))
            (scratchpad.vars["steps"] as ArrayNode).add("Observation: $fallback")
            return ToolExecutableResult(fallback, false)
        }
        val toolInput = responseOp["Action Input"]?.trim()
            ?.ifBlank { completion.substringAfter("Action Input:").trim() }
            ?.ifBlank { null }
        if (toolInput.isNullOrBlank()) {
            val fallback ="Tool input missing or malformed."
            emit(AgentChatEvent.Error(IllegalArgumentException(fallback)))
            (scratchpad.vars["steps"] as ArrayNode).add("Observation: $fallback")
            return ToolExecutableResult(fallback, false)
        }

        // execute tool
        val inputJson = MAPPER.createObjectNode().put("input", toolInput)
        emit(AgentChatEvent.UsingTool(tool.name, toolInput))
        val executionResult = tool.execute(inputJson, ExecContext())

        // log and return result
        val resultText = executionResult.get("result")?.asText() ?: ""
        emit(AgentChatEvent.ToolResult(tool.name, resultText))
        (scratchpad.vars["steps"] as ArrayNode).add("Observation: $resultText")

        return ToolExecutableResult(
            resultText,
            isTerminal = executionResult.get("isTerminal")?.asBoolean() ?: false,
        )
    }

}