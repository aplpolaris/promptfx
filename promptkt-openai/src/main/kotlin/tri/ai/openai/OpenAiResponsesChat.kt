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
import kotlinx.serialization.json.*
import tri.ai.core.*
import tri.ai.prompt.trace.AiPromptTrace

/** Chat completion using the OpenAI Responses API. */
class OpenAiResponsesChat(override val modelId: String, val client: OpenAiAdapter = OpenAiAdapter.INSTANCE) :
    MultimodalChat {

    override fun toString() = modelId

    override suspend fun chat(
        messages: List<MultimodalChatMessage>,
        parameters: MChatParameters
    ): AiPromptTrace {
        val request = responsesRequest(modelId, messages, parameters)
        return client.responseCompletion(request)
    }

    override fun close() {
        client.client.close()
    }

    companion object {
        private const val DEFAULT_MAX_TOKENS = 500

        internal fun responsesRequest(
            modelId: String,
            messages: List<MultimodalChatMessage>,
            parameters: MChatParameters
        ): ResponseRequest {
            // Extract system message from messages if present
            val systemMessage = messages.firstOrNull { it.role == MChatRole.System }
            val instructions = systemMessage?.content
                ?.firstOrNull { it.partType == MPartType.TEXT }?.text

            // Build input items from non-system messages
            val nonSystemMessages = messages.filter { it.role != MChatRole.System }
            val input = if (nonSystemMessages.size == 1 && nonSystemMessages[0].isSingleTextUser()) {
                ResponseInput(nonSystemMessages[0].content!![0].text!!)
            } else {
                ResponseInput(nonSystemMessages.map { it.toResponseInputItem() })
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

        /** Returns true if this is a simple single-text user message. */
        private fun MultimodalChatMessage.isSingleTextUser(): Boolean {
            val c = content
            return role == MChatRole.User && c != null && c.size == 1 && c[0].partType == MPartType.TEXT
        }

        /** Convert a [MultimodalChatMessage] to a [ResponseInputItem]. */
        private fun MultimodalChatMessage.toResponseInputItem(): ResponseInputItem {
            val contentArray = buildJsonArray {
                content?.forEach { part ->
                    when (part.partType) {
                        MPartType.TEXT -> add(buildJsonObject {
                            put("type", if (role == MChatRole.Assistant) "output_text" else "input_text")
                            put("text", part.text ?: "")
                        })
                        MPartType.IMAGE -> add(buildJsonObject {
                            put("type", "input_image")
                            // Responses API uses "image_url" for both URL and base64 data URLs
                            put("image_url", part.inlineData ?: "")
                        })
                        else -> { /* skip unsupported part types */ }
                    }
                }
            }
            return ResponseInputItem(
                id = "", // id is optional for input items; the API assigns ids to output items
                type = "message",
                status = null,
                role = role.responsesRole(),
                content = contentArray
            )
        }

        /** Convert [MChatRole] to Responses API role string. */
        private fun MChatRole.responsesRole() = when (this) {
            MChatRole.User -> "user"
            MChatRole.Assistant -> "assistant"
            MChatRole.System -> "system"
            else -> "user"
        }
    }

}
