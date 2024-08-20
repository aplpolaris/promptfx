/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
import com.aallam.openai.api.model.ModelId
import kotlinx.serialization.SerializationException
import tri.ai.openai.OpenAiClient
import tri.util.*

/**
 * Executes a prompt using tools and OpenAI. This will attempt to use tools in sequence as needed until a response
 * is achieved, at which point the system will return the final response. This may also ask the user to clarify their
 * query if needed.
 */
class JsonToolExecutor(val client: OpenAiClient, val model: String, val tools: List<JsonTool>) {

    private val chatTools = tools.mapNotNull {
        try {
            val params = it.jsonSchemaAsParameters()
            com.aallam.openai.api.chat.Tool.function(it.name, it.description, params)
        } catch (x: SerializationException) {
            warning<JsonToolExecutor>("Invalid JSON schema", x)
            null
        }
    }

    suspend fun execute(query: String) {
        info<JsonToolExecutor>("User Question: $ANSI_YELLOW$query$ANSI_RESET")
        val messages = mutableListOf(
            ChatMessage(ChatRole.System, SYSTEM_MESSAGE_1),
            ChatMessage(ChatRole.User, query)
        )

        var response = client.chat(ChatCompletionRequest(
            model = ModelId(this@JsonToolExecutor.model),
            messages = messages,
            tools = this@JsonToolExecutor.chatTools.ifEmpty { null }
        )).values!![0]
        messages += response
        var toolCalls = response.toolCalls as? List<ToolCall.Function>

        while (!toolCalls.isNullOrEmpty()) {
            if (toolCalls.size != 1)
                info<JsonToolExecutor>("${ANSI_RED}WARNING: expected a single tool to call, but found ${toolCalls.size}$ANSI_RESET")

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
                    val result = tool.run(json)
                    info<JsonToolExecutor>("Result: $ANSI_GREEN${result}$ANSI_RESET")

                    // add result to message history and call again
                    messages += ChatMessage(ChatRole.Tool, toolCallId = call.id, name = call.function.name, content = result)
                    response = client.chat(ChatCompletionRequest(
                        model = ModelId(this@JsonToolExecutor.model),
                        messages = messages,
                        tools = this@JsonToolExecutor.chatTools.ifEmpty { null }
                    )).values!![0]
                    messages += response
                    toolCalls = response.toolCalls as? List<ToolCall.Function>
                } else {
                    toolCalls = listOf()
                }
            }
        }

        info<JsonToolExecutor>("Final Response: $ANSI_GREEN${response.content}$ANSI_RESET")
    }

    companion object {
        private const val SYSTEM_MESSAGE_1 =
            "Don't make assumptions about what values to plug into functions. " +
            "Ask for clarification if a user request is ambiguous. " +
            "Don't attempt to answer questions that are outside the scope of the functions. "

        private fun FunctionCall.tryJson() = try {
            argumentsAsJson()
        } catch (x: SerializationException) {
            null
        }
    }

}
