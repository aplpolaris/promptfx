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
package tri.ai.anthropic

import tri.ai.core.*
import tri.ai.anthropic.AnthropicModelIndex.CLAUDE_3_5_SONNET
import tri.ai.prompt.trace.AiOutput
import tri.ai.prompt.trace.AiPromptTrace

/** Multimodal chat with Anthropic models. */
class AnthropicMultimodalChat(override val modelId: String = CLAUDE_3_5_SONNET, val client: AnthropicAdapter = AnthropicAdapter.INSTANCE) : MultimodalChat {

    override fun toString() = modelId

    override suspend fun chat(
        messages: List<MultimodalChatMessage>,
        parameters: MChatParameters
    ): AiPromptTrace {
        
        // Convert multimodal messages to Anthropic format
        val systemMessages = messages.filter { it.role == MChatRole.System }
        val conversationMessages = messages.filter { it.role != MChatRole.System }
        
        val systemPrompt = if (systemMessages.isNotEmpty()) {
            systemMessages.joinToString("\n") { msg ->
                msg.content?.joinToString("\n") { part -> part.text ?: "" } ?: ""
            }
        } else null
        
        val anthropicMessages = conversationMessages.map { msg ->
            AnthropicMessage(
                role = when (msg.role) {
                    MChatRole.User -> "user"
                    MChatRole.Assistant -> "assistant"
                    else -> "user"
                },
                content = msg.content?.map { part ->
                    when (part.partType) {
                        MPartType.TEXT -> AnthropicContent(type = "text", text = part.text)
                        MPartType.IMAGE -> if (part.inlineData?.startsWith("data:") == true) {
                            // Handle base64 encoded images
                            val mediaType = part.inlineData.substringAfter("data:").substringBefore(";")
                            val base64Data = part.inlineData.substringAfter("base64,")
                            AnthropicImageContent(
                                source = AnthropicImageSource(
                                    type = "base64",
                                    mediaType = mediaType,
                                    data = base64Data
                                )
                            )
                        } else {
                            // If not base64, treat as text description
                            AnthropicContent(type = "text", text = part.text ?: "[Image]")
                        }
                        MPartType.TOOL_CALL, MPartType.TOOL_RESPONSE -> {
                            // Convert tool calls/responses to text for now
                            AnthropicContent(type = "text", text = part.text ?: "[Tool]")
                        }
                    }
                } ?: listOf(AnthropicContent(type = "text", text = ""))
            )
        }
        
        val request = AnthropicChatRequest(
            model = modelId,
            messages = anthropicMessages,
            maxTokens = parameters.tokens ?: 1024,
            temperature = parameters.variation.temperature,
            topP = parameters.variation.topP,
            stopSequences = parameters.stop,
            system = systemPrompt
        )
        
        return client.chatCompletion(request)
            .mapOutput { output ->
                when (output.message) {
                    is TextChatMessage -> AiOutput(multimodalMessage = MultimodalChatMessage(MChatRole.Assistant, listOf(MChatMessagePart(text = output.message.content))))
                    else -> AiOutput(multimodalMessage = MultimodalChatMessage(MChatRole.Assistant, listOf(MChatMessagePart(text = output.text ?: ""))))
                }
            }
    }

}

// Additional data classes for image support
data class AnthropicImageContent(
    val source: AnthropicImageSource
) : AnthropicContent("image")

data class AnthropicImageSource(
    val type: String,
    @com.fasterxml.jackson.annotation.JsonProperty("media_type") val mediaType: String,
    val data: String
)