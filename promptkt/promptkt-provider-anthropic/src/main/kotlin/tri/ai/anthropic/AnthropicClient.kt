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
package tri.ai.anthropic

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tri.ai.core.MChatRole
import java.io.Closeable

/**
 * General purpose client for the Anthropic Messages API.
 * See https://docs.anthropic.com/en/api/messages
 */
class AnthropicClient : Closeable {

    val settings = AnthropicSettings()
    private val client get() = settings.client

    /** Send a request to the Anthropic Messages API. */
    suspend fun createMessage(request: AnthropicMessageRequest): AnthropicMessageResponse {
        return client.post("messages") {
            setBody(request)
        }.body<AnthropicMessageResponse>()
    }

    override fun close() {
        client.close()
    }

    companion object {
        val INSTANCE by lazy { AnthropicClient() }

        /** Convert [MChatRole] to Anthropic role string. */
        fun MChatRole.toAnthropicRole() = when (this) {
            MChatRole.User -> "user"
            MChatRole.Assistant -> "assistant"
            else -> error("Invalid role for Anthropic messages: $this")
        }
    }
}

//region REQUEST DTOs

/** Request body for POST /v1/messages. */
@Serializable
data class AnthropicMessageRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val messages: List<AnthropicMessage>,
    val system: String? = null,
    val temperature: Double? = null,
    @SerialName("top_p") val topP: Double? = null,
    @SerialName("top_k") val topK: Int? = null,
    @SerialName("stop_sequences") val stopSequences: List<String>? = null
)

/** A single message in the Anthropic conversation. */
@Serializable
data class AnthropicMessage(
    val role: String,
    val content: List<AnthropicContentBlock>
) {
    companion object {
        fun text(role: String, text: String) = AnthropicMessage(role, listOf(AnthropicContentBlock.text(text)))
    }
}

/** A content block within an Anthropic message. */
@Serializable
data class AnthropicContentBlock(
    val type: String,
    val text: String? = null,
    val source: AnthropicImageSource? = null
) {
    companion object {
        fun text(text: String) = AnthropicContentBlock(type = "text", text = text)
        fun image(mediaType: String, base64Data: String) =
            AnthropicContentBlock(type = "image", source = AnthropicImageSource("base64", mediaType, base64Data))
    }
}

/** Source for an image content block. */
@Serializable
data class AnthropicImageSource(
    val type: String,
    @SerialName("media_type") val mediaType: String,
    val data: String
)

//endregion

//region RESPONSE DTOs

/** Response from POST /v1/messages. */
@Serializable
data class AnthropicMessageResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<AnthropicResponseBlock>,
    val model: String,
    @SerialName("stop_reason") val stopReason: String? = null,
    @SerialName("stop_sequence") val stopSequence: String? = null,
    val usage: AnthropicUsage? = null,
    val error: AnthropicError? = null
) {
    /** Extract the first text response. */
    fun firstText(): String? = content.firstOrNull { it.type == "text" }?.text

    /** Extract all text responses. */
    fun allText(): List<String> = content.filter { it.type == "text" }.mapNotNull { it.text }
}

/** A content block in the Anthropic response. */
@Serializable
data class AnthropicResponseBlock(
    val type: String,
    val text: String? = null
)

/** Token usage information. */
@Serializable
data class AnthropicUsage(
    @SerialName("input_tokens") val inputTokens: Int,
    @SerialName("output_tokens") val outputTokens: Int
)

/** Error information returned by the API. */
@Serializable
data class AnthropicError(
    val type: String,
    val message: String
)

/** Top-level error response from the API. */
@Serializable
data class AnthropicErrorResponse(
    val type: String,
    val error: AnthropicError
)

//endregion
