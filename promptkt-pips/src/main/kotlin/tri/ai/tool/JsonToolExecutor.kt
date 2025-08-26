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

import com.aallam.openai.api.chat.FunctionCall
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.serialization.SerializationException
import tri.ai.core.*
import tri.ai.pips.core.ExecContext
import tri.ai.pips.core.Executable
import tri.ai.pips.core.MAPPER
import tri.ai.tool.JsonMultimodalToolExecutor.Companion.tryJson
import tri.util.*

/**
 * Executes a prompt using tools and a [MultimodalChat]. This will attempt to use tools in sequence as needed until a response
 * is achieved, at which point the system will return the final response. This may also ask the user to clarify their
 * query if needed.
 */
class JsonToolExecutor(val chat: MultimodalChat, val tools: List<Executable>) {

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

    suspend fun execute(query: String): String {
        info<JsonToolExecutor>("User Question: $ANSI_YELLOW$query$ANSI_RESET")
        val systemMessage = PROMPTS.get("tools/json-tool-system-message")!!.template!!
        val messages = mutableListOf(
            MultimodalChatMessage.text(MChatRole.System, systemMessage),
            MultimodalChatMessage.text(MChatRole.User, query)
        )

        var response = chat.chat(messages, MChatParameters(tools = MChatTools(tools = chatTools)))
            .firstValue.multimodalMessage!!
        messages += response
        var toolCalls = response.toolCalls

        while (!toolCalls.isNullOrEmpty()) {
            // print interim results
            toolCalls.forEach { call ->
                info<JsonToolExecutor>("Call Function: $ANSI_CYAN${call.name}$ANSI_RESET with parameters $ANSI_CYAN${call.argumentsAsJson}$ANSI_RESET")
                val tool = tools.firstOrNull { it.name == call.name }
                if (tool == null) {
                    info<JsonToolExecutor>("${ANSI_RED}Unknown tool: ${call.name}$ANSI_RESET")
                }
                val json = call.tryJson()
                if (json == null) {
                    info<JsonToolExecutor>("${ANSI_RED}Invalid JSON: ${call.name}$ANSI_RESET")
                }
                if (tool != null && json != null) {
                    val context = ExecContext()
                    val result = tool.execute(json, context)
                    val resultText = result.get("result")?.asText() ?: result.toString()
                    info<JsonToolExecutor>("Result: $ANSI_GREEN${resultText}$ANSI_RESET")

                    // add result to message history and call again
                    messages += MultimodalChatMessage.tool(resultText, call.id)
                }
            }

            response = chat.chat(messages = messages, MChatParameters(tools = MChatTools(tools = chatTools)))
                .firstValue.multimodalMessage!!
            messages += response
            toolCalls = response.toolCalls
        }

        val finalContent = response.content?.first()?.text
        info<JsonToolExecutor>("Final Response: $ANSI_GREEN${finalContent}$ANSI_RESET")
        return finalContent!!
    }

    companion object {
        private fun FunctionCall.tryJson(): JsonNode? = try {
            val jsonObject = argumentsAsJson()
            // Convert kotlinx JsonObject to Jackson JsonNode
            val jsonString = jsonObject.toString()
            MAPPER.readTree(jsonString)
        } catch (x: SerializationException) {
            null
        } catch (x: Exception) {
            null
        }
    }

}
