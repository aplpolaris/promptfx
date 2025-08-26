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

import com.aallam.openai.api.chat.*
import com.aallam.openai.api.core.Parameters
import com.aallam.openai.api.model.ModelId
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.serialization.SerializationException
import tri.ai.openai.OpenAiAdapter
import tri.ai.pips.core.ExecContext
import tri.ai.pips.core.Executable
import tri.ai.pips.core.MAPPER
import tri.util.*

/**
 * Executes a prompt using tools and OpenAI. This will attempt to use tools in sequence as needed until a response
 * is achieved, at which point the system will return the final response. This may also ask the user to clarify their
 * query if needed.
 */
class JsonToolExecutor(val client: OpenAiAdapter, val model: String, val tools: List<Executable>) {

    private val chatTools = tools.mapNotNull {
        val params = try {
            it.inputSchema?.let { schema -> Parameters.fromJsonString(MAPPER.writeValueAsString(schema)) }
        } catch (x: SerializationException) {
            warning<JsonToolExecutor>("Invalid JSON schema: ${it.inputSchema}", x)
            null
        }
        if (params == null) null else
            com.aallam.openai.api.chat.Tool.function(it.name, it.description, params)
    }

    suspend fun execute(query: String): String {
        info<JsonToolExecutor>("User Question: $ANSI_YELLOW$query$ANSI_RESET")
        val systemMessage = PROMPTS.get("tools/json-tool-system-message")!!.template
        val messages = mutableListOf(
            ChatMessage(role = ChatRole.System, content = systemMessage),
            ChatMessage(role = ChatRole.User, content = query)
        )

        var response = client.chat(ChatCompletionRequest(
            model = ModelId(this@JsonToolExecutor.model),
            messages = messages,
            tools = this@JsonToolExecutor.chatTools.ifEmpty { null }
        )).firstValue
        messages += response
        var toolCalls = response.toolCalls as? List<ToolCall.Function>

        while (!toolCalls.isNullOrEmpty()) {
//            if (toolCalls.size != 1)
//                info<JsonToolExecutor>("${ANSI_RED}WARNING: expected a single tool to call, but found ${toolCalls.size}$ANSI_RESET")

            // print interim results
            toolCalls.forEach { call ->
                info<JsonToolExecutor>("Call Function: $ANSI_CYAN${call.function.name}$ANSI_RESET with parameters $ANSI_CYAN${call.function.arguments}$ANSI_RESET")
                val tool = tools.firstOrNull { it.name == call.function.name }
                if (tool == null) {
                    info<JsonToolExecutor>("${ANSI_RED}Unknown tool: ${call.function.name}$ANSI_RESET")
                }
                val json = call.function.tryJson()
                if (json == null) {
                    info<JsonToolExecutor>("${ANSI_RED}Invalid JSON: ${call.function.name}$ANSI_RESET")
                }
                if (tool != null && json != null) {
                    val context = ExecContext()
                    val result = tool.execute(json, context)
                    val resultText = result.get("result")?.asText() ?: result.toString()
                    info<JsonToolExecutor>("Result: $ANSI_GREEN${resultText}$ANSI_RESET")

                    // add result to message history and call again
                    messages += ChatMessage(ChatRole.Tool, toolCallId = call.id, name = call.function.name, content = resultText)
                }
            }

            response = client.chat(ChatCompletionRequest(
                model = ModelId(this@JsonToolExecutor.model),
                messages = messages,
                tools = this@JsonToolExecutor.chatTools.ifEmpty { null }
            )).firstValue
            messages += response
            toolCalls = response.toolCalls as? List<ToolCall.Function>
        }

        info<JsonToolExecutor>("Final Response: $ANSI_GREEN${response.content}$ANSI_RESET")
        return response.content!!
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
