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
package tri.ai.tool

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import tri.ai.core.*
import tri.util.*

/**
 * Executes a prompt using tools and OpenAI. This will attempt to use tools in sequence as needed until a response
 * is achieved, at which point the system will return the final response. This may also ask the user to clarify their
 * query if needed.
 */
class JsonMultimodalToolExecutor(val model: MultimodalChat, val tools: List<JsonTool>) {

    private val chatTools = tools.map { it.tool }

    suspend fun execute(query: String): String {
        info<JsonMultimodalToolExecutor>("User Question: $ANSI_YELLOW$query$ANSI_RESET")
        val systemMessage = PROMPTS.fill("json-tool-system-message")
        val messages = mutableListOf(
            MultimodalChatMessage.text(MChatRole.System, systemMessage),
            MultimodalChatMessage.text(MChatRole.User, query)
        )

        var response = model.chat(
            messages,
            MChatParameters(tools = MChatTools(tools = chatTools))
        ).firstValue
        messages += response
        var toolCalls = response.toolCalls

        while (!toolCalls.isNullOrEmpty()) {
            // print interim results
            toolCalls.forEach { call ->
                info<JsonMultimodalToolExecutor>("Call Function: $ANSI_CYAN${call.name}$ANSI_RESET with parameters $ANSI_CYAN${call.argumentsAsJson}$ANSI_RESET")
                val tool = tools.firstOrNull { it.tool.name == call.name }
                if (tool == null) {
                    info<JsonMultimodalToolExecutor>("${ANSI_RED}Unknown tool: ${call.name}$ANSI_RESET")
                }
                val json = call.tryJson()
                if (json == null) {
                    info<JsonMultimodalToolExecutor>("${ANSI_RED}Invalid JSON: ${call.name}$ANSI_RESET")
                }
                if (tool != null && json != null) {
                    val result = tool.run(json)
                    info<JsonMultimodalToolExecutor>("Result: $ANSI_GREEN${result}$ANSI_RESET")

                    // add result to message history and call again
                    messages += MultimodalChatMessage.tool(result, call.id)
                }
            }

            response = model.chat(
                messages,
                MChatParameters(tools = MChatTools(tools = chatTools))
            ).firstValue
            messages += response
            toolCalls = response.toolCalls
        }

        info<JsonMultimodalToolExecutor>("Final Response: $ANSI_GREEN${response.content?.first()?.text}$ANSI_RESET")
        return response.content?.first()?.text ?: "(unable to generate response)"
    }

    companion object {
        private fun MToolCall.tryJson() = try {
            Json.parseToJsonElement(argumentsAsJson) as? JsonObject
        } catch (x: SerializationException) {
            null
        }
    }

}
