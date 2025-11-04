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

import kotlinx.coroutines.flow.FlowCollector
import tri.ai.core.*
import tri.ai.core.agent.*
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.Executable
import tri.ai.core.tool.createTool
import tri.util.tryJson

/**
 * Executes a prompt using tools and a [MultimodalChat]. This will attempt to use tools in sequence as needed until a response
 * is achieved, at which point the system will return the final response.
 */
class JsonToolExecutor(tools: List<Executable>) : AgentToolChatSupport(tools) {

    val params = MChatParameters(tools = MChatTools(tools = tools.mapNotNull { it.createTool() }))

    override suspend fun FlowCollector<AgentChatEvent>.sendMessageSafe(session: AgentChatSession, message: MultimodalChatMessage): AgentChatResponse {
        val question = logTextContent(message)
        updateSession(message, session)
        logToolUsage()

        // prepare chat and message history
        val chat = findMultimodalChat(session, this)
        val systemMessage = PROMPTS.get("tools/json-tool-system-message")!!.template!!
        val messages = mutableListOf(
            MultimodalChatMessage.text(MChatRole.System, systemMessage),
            MultimodalChatMessage.text(MChatRole.User, question)
        )

        var response = chat.chat(messages, params).firstValue.multimodalMessage!!
        messages += response
        var toolCalls = response.toolCalls

        while (!toolCalls.isNullOrEmpty()) {
            // print interim results
            toolCalls.forEach { call ->
                emitUsingTool(call.name, call.argumentsAsJson)
                val tool = tools.firstOrNull { it.name == call.name }
                val json = call.argumentsAsJson.tryJson()
                if (tool == null) {
                    emitError(NullPointerException("Unknown tool: ${call.name}"))
                }
                if (json == null) {
                    emitError(IllegalArgumentException("Invalid or missing json: $json"))
                }
                if (tool != null && json != null) {
                    val context = ExecContext()
                    val result = tool.execute(json, context)
                    val resultText = result.get("result")?.asText() ?: result.toString()
                    emitToolResult(call.name, resultText)

                    // add result to message history and call again
                    messages += MultimodalChatMessage.tool(resultText, call.id)
                }
            }

            response = chat.chat(messages = messages, params).firstValue.multimodalMessage!!
            messages += response
            toolCalls = response.toolCalls
        }

        // store and log response, maybe update session name
        updateSession(response, session, updateName = true)
        return agentChatResponse(response, session)
    }

}
