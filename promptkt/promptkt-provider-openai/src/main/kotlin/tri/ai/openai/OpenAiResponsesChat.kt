/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.openai

import com.aallam.openai.api.model.ModelId
import com.aallam.openai.api.response.ResponseInput
import com.aallam.openai.api.response.ResponseInputItem
import com.aallam.openai.api.response.ResponseRequest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import tri.ai.core.*
import tri.ai.prompt.trace.AiPromptTrace

/** Chat completion using OpenAI Responses API. */
class OpenAiResponsesChat(
    override val modelId: String,
    override val modelSource: String = OpenAiModelIndex.MODEL_SOURCE,
    val client: OpenAiAdapter = OpenAiAdapter.INSTANCE
) : MultimodalChat {

    override fun toString() = modelDisplayName()

    override suspend fun chat(
        messages: List<MultimodalChatMessage>,
        parameters: MChatParameters
    ): AiPromptTrace {
        val request = buildResponseRequest(modelId, messages, parameters)
        return client.responseCompletion(request)
    }

    override fun close() {
        client.client.close()
    }

    companion object {
        private const val DEFAULT_MAX_TOKENS = 1000

        /** Build a [ResponseRequest] from a model id, chat messages, and parameters. */
        fun buildResponseRequest(
            modelId: String,
            messages: List<MultimodalChatMessage>,
            parameters: MChatParameters
        ): ResponseRequest {
            // Extract system messages for the instructions parameter
            val systemMessages = messages.filter { it.role == MChatRole.System }
            val otherMessages = messages.filter { it.role != MChatRole.System }

            val instructions = systemMessages
                .mapNotNull { it.content?.mapNotNull { p -> p.text }?.joinToString("\n") }
                .joinToString("\n")
                .ifEmpty { null }

            val input = when {
                otherMessages.isEmpty() -> ResponseInput("")
                otherMessages.size == 1 && otherMessages[0].role == MChatRole.User && otherMessages[0].hasTextOnly() ->
                    ResponseInput(otherMessages[0].textContent())
                else -> ResponseInput(otherMessages.map { it.toResponseInputItem() })
            }

            return ResponseRequest(
                model = ModelId(modelId),
                input = input,
                instructions = instructions,
                maxOutputTokens = parameters.tokens ?: DEFAULT_MAX_TOKENS,
                temperature = parameters.variation.temperature,
                topP = parameters.variation.topP
            )
        }

        /** Returns true if the message has text content only (no images or other modalities). */
        private fun MultimodalChatMessage.hasTextOnly() =
            content?.all { it.partType == MPartType.TEXT } != false

        /** Returns the concatenated text content of this message. */
        private fun MultimodalChatMessage.textContent() =
            content?.mapNotNull { it.text }?.joinToString("\n") ?: ""

        /** Convert a [MultimodalChatMessage] to a [ResponseInputItem] for the Responses API. */
        private fun MultimodalChatMessage.toResponseInputItem(): ResponseInputItem {
            val roleStr = when (role) {
                MChatRole.User -> "user"
                MChatRole.Assistant -> "assistant"
                MChatRole.System -> "developer"
                MChatRole.Tool -> "tool"
                MChatRole.None -> "user"
            }
            val contentParts = content
            val contentJson = when {
                contentParts.isNullOrEmpty() -> JsonPrimitive("")
                contentParts.size == 1 && contentParts[0].partType == MPartType.TEXT ->
                    JsonPrimitive(contentParts[0].text ?: "")
                else -> buildJsonArray {
                    contentParts.forEach { part ->
                        when {
                            part.text != null -> add(buildJsonObject {
                                put("type", "input_text")
                                put("text", part.text)
                            })
                            part.inlineData != null -> add(buildJsonObject {
                                put("type", "input_image")
                                put("image_url", buildJsonObject {
                                    put("url", part.inlineData)
                                })
                            })
                        }
                    }
                }
            }
            return ResponseInputItem(
                type = "message",
                role = roleStr,
                content = contentJson
            )
        }
    }

}
