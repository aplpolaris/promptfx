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
package tri.ai.anthropicsdk

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.*
import tri.ai.core.*
import java.io.Closeable

/**
 * Client for the Anthropic API using the official anthropic-java SDK.
 * See https://github.com/anthropics/anthropic-sdk-java
 */
class AnthropicSdkClient : Closeable {

    val settings = AnthropicSdkSettings()
    private var client: AnthropicClient? = null

    init {
        if (settings.isConfigured()) {
            client = AnthropicOkHttpClient.builder()
                .apiKey(settings.apiKey!!)
                .build()
        }
    }

    fun isConfigured() = settings.isConfigured() && client != null

    /**
     * Send a message request to Anthropic and return the response.
     * @param modelId the model to use
     * @param messages list of conversation messages (system, user, assistant)
     * @param maxTokens maximum number of tokens to generate
     * @param variation sampling parameters (temperature, topP, topK)
     * @param stop stop sequences
     */
    fun createMessage(
        modelId: String,
        messages: List<MultimodalChatMessage>,
        maxTokens: Int = 1024,
        variation: MChatVariation = MChatVariation(),
        stop: List<String>? = null
    ): Message {
        val c = client ?: throw IllegalStateException("Anthropic client not initialized")

        val systemText = messages
            .filter { it.role == MChatRole.System }
            .mapNotNull { msg -> msg.content?.firstOrNull()?.text }
            .joinToString("\n")
            .takeIf { it.isNotBlank() }

        val conversationMessages = messages
            .filter { it.role != MChatRole.System }
            .map { it.toMessageParam() }

        val paramsBuilder = MessageCreateParams.builder()
            .model(modelId)
            .maxTokens(maxTokens.toLong())
            .messages(conversationMessages)

        systemText?.let { paramsBuilder.system(it) }
        variation.temperature?.let { paramsBuilder.temperature(it) }
        variation.topP?.let { paramsBuilder.topP(it) }
        stop?.takeIf { it.isNotEmpty() }?.let { paramsBuilder.stopSequences(it) }

        return c.messages().create(paramsBuilder.build())
    }

    override fun close() {
        client?.close()
    }

    companion object {
        val INSTANCE by lazy { AnthropicSdkClient() }

        /** Convert a [MultimodalChatMessage] to an Anthropic [MessageParam]. */
        fun MultimodalChatMessage.toMessageParam(): MessageParam {
            val role = when (this.role) {
                MChatRole.User -> MessageParam.Role.USER
                MChatRole.Assistant -> MessageParam.Role.ASSISTANT
                else -> MessageParam.Role.USER
            }
            val parts = this.content ?: listOf()
            return if (parts.size == 1 && parts.first().partType == MPartType.TEXT) {
                // Simple text-only case
                MessageParam.builder()
                    .role(role)
                    .content(parts.first().text ?: "")
                    .build()
            } else {
                // Multi-part or non-text content
                val blocks = parts.map { it.toContentBlockParam() }
                MessageParam.builder()
                    .role(role)
                    .contentOfBlockParams(blocks)
                    .build()
            }
        }

        /** Convert a [MChatMessagePart] to an Anthropic [ContentBlockParam]. */
        fun MChatMessagePart.toContentBlockParam(): ContentBlockParam = when (partType) {
            MPartType.TEXT -> ContentBlockParam.ofText(
                TextBlockParam.builder().text(text ?: "").build()
            )
            MPartType.IMAGE -> ContentBlockParam.ofImage(
                buildImageBlockParam(inlineData!!)
            )
            MPartType.TOOL_CALL -> ContentBlockParam.ofToolUse(
                ToolUseBlockParam.builder()
                    .id(functionName ?: "")
                    .name(functionName ?: "")
                    .input(
                        ToolUseBlockParam.Input.builder()
                            .putAllAdditionalProperties(
                                (functionArgs ?: emptyMap()).mapValues { com.anthropic.core.JsonValue.from(it.value) }
                            )
                            .build()
                    )
                    .build()
            )
            MPartType.TOOL_RESPONSE -> ContentBlockParam.ofToolResult(
                ToolResultBlockParam.builder()
                    .toolUseId(functionName ?: "")
                    .content((functionArgs ?: emptyMap()).entries.joinToString("\n") { (k, v) -> "$k: $v" })
                    .build()
            )
        }

        /** Parse a data URL (data:<mimeType>;base64,<data>) into an [ImageBlockParam]. */
        private fun buildImageBlockParam(dataUrl: String): ImageBlockParam {
            require(dataUrl.startsWith("data:") && dataUrl.contains(";base64,")) {
                "Invalid data URL format. Expected: data:<mimeType>;base64,<data>"
            }
            val mimeType = dataUrl.substringBefore(";base64,").substringAfter("data:")
            val base64Data = dataUrl.substringAfter(";base64,")
            val mediaType = Base64ImageSource.MediaType.of(mimeType)
            return ImageBlockParam.builder()
                .source(
                    Base64ImageSource.builder()
                        .data(base64Data)
                        .mediaType(mediaType)
                        .build()
                )
                .build()
        }

        /** Extract all text from an Anthropic [Message] response. */
        fun Message.extractText(): String =
            content().filterIsInstance<ContentBlock>()
                .filter { it.isText() }
                .mapNotNull { it.text().orElse(null)?.text() }
                .joinToString("")

        /** Convert a [ContentBlock] to a [MChatMessagePart]. */
        fun ContentBlock.toMChatMessagePart(): MChatMessagePart = when {
            isText() -> MChatMessagePart.text(text().get().text())
            isToolUse() -> {
                val tu = toolUse().get()
                // Parse tool inputs: convert JsonValue string representation to Map<String, String>
                val inputStr = tu._input().toString()
                MChatMessagePart.toolCall(tu.name(), mapOf("input" to inputStr))
            }
            else -> MChatMessagePart.text("[unsupported content block]")
        }
    }

}
