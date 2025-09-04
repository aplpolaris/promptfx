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

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tri.ai.core.TextChatMessage
import tri.ai.core.MChatRole
import tri.ai.core.MChatVariation
import tri.ai.core.VisionLanguageChatMessage
import java.io.Closeable

/**
 * General purpose client for the Anthropic API.
 * See https://docs.anthropic.com/claude/reference/messages_post
 */
class AnthropicClient : Closeable {

    val settings = AnthropicSettings()
    private val client = settings.client

    /** Returns true if the client is configured with an API key. */
    fun isConfigured() = settings.apiKey.isNotBlank()

    //region CORE API METHODS

    suspend fun createMessage(request: CreateMessageRequest): CreateMessageResponse {
        return client.post("messages") {
            setBody(request)
        }.body<CreateMessageResponse>()
    }

    //endregion

    //region CONVENIENCE METHODS

    suspend fun createMessage(
        model: String,
        messages: List<TextChatMessage>,
        systemMessage: String? = null,
        maxTokens: Int = 1024,
        variation: MChatVariation? = null
    ): CreateMessageResponse {
        val anthropicMessages = messages.map { it.toAnthropicMessage() }
        val request = CreateMessageRequest(
            model = model,
            maxTokens = maxTokens,
            messages = anthropicMessages,
            system = systemMessage,
            temperature = variation?.temperature,
            topP = variation?.topP
        )
        return createMessage(request)
    }

    suspend fun createVisionMessage(
        model: String,
        messages: List<VisionLanguageChatMessage>,
        systemMessage: String? = null,
        maxTokens: Int = 1024,
        variation: MChatVariation? = null
    ): CreateMessageResponse {
        val anthropicMessages = messages.map { it.toAnthropicVisionMessage() }
        val request = CreateMessageRequest(
            model = model,
            maxTokens = maxTokens,
            messages = anthropicMessages,
            system = systemMessage,
            temperature = variation?.temperature,
            topP = variation?.topP
        )
        return createMessage(request)
    }

    //endregion

    override fun close() {
        client.close()
    }

    companion object {
        val INSTANCE by lazy { AnthropicClient() }

        /** Convert from [MChatRole] to string representing Anthropic role. */
        fun MChatRole.toAnthropicRole() = when (this) {
            MChatRole.User -> "user"
            MChatRole.Assistant -> "assistant"
            else -> error("Anthropic API only supports user and assistant roles, got: $this")
        }

        /** Convert from string representing Anthropic role to [MChatRole]. */
        fun String.fromAnthropicRole() = when (this) {
            "user" -> MChatRole.User
            "assistant" -> MChatRole.Assistant
            else -> error("Invalid Anthropic role: $this")
        }

        /** Convert [TextChatMessage] to [AnthropicMessage]. */
        fun TextChatMessage.toAnthropicMessage() = AnthropicMessage(
            role = role.toAnthropicRole(),
            content = listOf(AnthropicContent.Text(content ?: ""))
        )

        /** Convert [VisionLanguageChatMessage] to [AnthropicMessage] with image support. */
        fun VisionLanguageChatMessage.toAnthropicVisionMessage(): AnthropicMessage {
            val content = mutableListOf<AnthropicContent>()
            
            // Add text content
            if (this.content.isNotBlank()) {
                content.add(AnthropicContent.Text(this.content))
            }
            
            // Add image content (assuming base64 data URL format)
            val imageStr = this.image?.toString()
            if (imageStr != null && imageStr.isNotBlank()) {
                val imageData = if (imageStr.startsWith("data:image/")) {
                    val parts = imageStr.split(",")
                    if (parts.size == 2) {
                        val mediaType = parts[0].substringAfter("data:").substringBefore(";")
                        AnthropicContent.Image(
                            source = AnthropicImageSource(
                                type = "base64",
                                mediaType = mediaType,
                                data = parts[1]
                            )
                        )
                    } else {
                        error("Invalid image data URL format")
                    }
                } else {
                    error("Image must be in data URL format")
                }
                content.add(imageData)
            }
            
            return AnthropicMessage(
                role = role.toAnthropicRole(),
                content = content
            )
        }
    }
}

//region DTOs - see https://docs.anthropic.com/claude/reference/messages_post

@Serializable
data class CreateMessageRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val messages: List<AnthropicMessage>,
    val system: String? = null,
    val temperature: Double? = null,
    @SerialName("top_p") val topP: Double? = null,
    @SerialName("top_k") val topK: Int? = null,
    @SerialName("stop_sequences") val stopSequences: List<String>? = null
)

@Serializable
data class CreateMessageResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<AnthropicContent>,
    val model: String,
    @SerialName("stop_reason") val stopReason: String?,
    @SerialName("stop_sequence") val stopSequence: String?,
    val usage: AnthropicUsage
)

@Serializable
data class AnthropicMessage(
    val role: String,
    val content: List<AnthropicContent>
)

@Serializable
sealed class AnthropicContent {
    @Serializable
    @SerialName("text")
    data class Text(val text: String) : AnthropicContent()
    
    @Serializable
    @SerialName("image")
    data class Image(val source: AnthropicImageSource) : AnthropicContent()
}

@Serializable
data class AnthropicImageSource(
    val type: String,
    @SerialName("media_type") val mediaType: String,
    val data: String
)

@Serializable
data class AnthropicUsage(
    @SerialName("input_tokens") val inputTokens: Int,
    @SerialName("output_tokens") val outputTokens: Int
)

//endregion