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

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import tri.ai.core.*
import tri.ai.core.agent.AgentChatEvent
import tri.ai.core.agent.AgentChatResponse
import tri.ai.core.agent.AgentChatSession
import tri.ai.core.agent.AgentToolChatSupport
import tri.ai.core.agent.MAPPER
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.Executable
import tri.util.*

/**
 * Executes a prompt using tools and a [MultimodalChat]. This will attempt to use tools in sequence as needed until a response
 * is achieved, at which point the system will return the final response.
 */
class JsonToolExecutor(tools: List<Executable>) : AgentToolChatSupport(tools) {

    private val chatTools = tools.mapNotNull {
        val schema = try {
            it.inputSchema?.let { MAPPER.writeValueAsString(it) }
        } catch (x: SerializationException) {
            warning<JsonToolExecutor>("Invalid JSON schema: ${it.inputSchema}", x)
            null
        }
        if (schema == null) null else
            MTool(it.name, it.description, schema)
    }

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

        var response = chat.chat(messages, MChatParameters(tools = MChatTools(tools = chatTools)))
            .firstValue.multimodalMessage!!
        messages += response
        var toolCalls = response.toolCalls

        while (!toolCalls.isNullOrEmpty()) {
            // print interim results
            toolCalls.forEach { call ->
                emit(AgentChatEvent.UsingTool(call.name, call.argumentsAsJson))
                val tool = tools.firstOrNull { it.name == call.name }
                if (tool == null) {
                    emit(AgentChatEvent.Error(NullPointerException("Unknown tool: ${call.name}")))
                }
                val json = call.tryJson()
                if (json == null) {
                    emit(AgentChatEvent.Error(IllegalArgumentException("Invalid or missing json: $json")))
                }
                if (tool != null && json != null) {
                    val context = ExecContext()
                    val result = tool.execute(json, context)
                    val resultText = result.get("result")?.asText() ?: result.toString()
                    emit(AgentChatEvent.ToolResult(call.name, resultText))

                    // add result to message history and call again
                    messages += MultimodalChatMessage.tool(resultText, call.id)
                }
            }

            response = chat.chat(messages = messages, MChatParameters(tools = MChatTools(tools = chatTools)))
                .firstValue.multimodalMessage!!
            messages += response
            toolCalls = response.toolCalls
        }

        // store and log response, maybe update session name
        updateSession(response, session, updateName = true)
        return agentChatResponse(response, session)
    }

    companion object {
        fun MToolCall.tryJson(): JsonNode? = try {
            val jsonObject = Json.parseToJsonElement(argumentsAsJson) as? JsonObject
            // Convert kotlinx JsonObject to Jackson JsonNode
            jsonObject?.let {
                val jsonString = it.toString()
                MAPPER.readTree(jsonString)
            }
        } catch (x: SerializationException) {
            null
        } catch (x: Exception) {
            null
        }
    }

}
