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

import tri.ai.anthropic.AnthropicClient.Companion.toAnthropicRole
import tri.ai.anthropic.AnthropicModelIndex.CLAUDE_SONNET_4_6
import tri.ai.core.MChatParameters
import tri.ai.core.MChatRole
import tri.ai.core.MPartType
import tri.ai.core.MultimodalChat
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.TextChatMessage
import tri.ai.prompt.trace.AiEnvInfo
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiTaskTrace

/** Multimodal chat with Anthropic Claude models (text and images). */
class AnthropicMultimodalChat(
    override val modelId: String = CLAUDE_SONNET_4_6,
    val client: AnthropicClient = AnthropicClient.INSTANCE
) : MultimodalChat {

    override val modelSource = AnthropicModelIndex.MODEL_SOURCE

    override fun toString() = modelDisplayName()

    override suspend fun chat(
        messages: List<MultimodalChatMessage>,
        parameters: MChatParameters
    ): AiPromptTrace {
        val modelInfo = AiModelInfo.info(modelId, tokens = parameters.tokens, stop = parameters.stop)
        val t0 = System.currentTimeMillis()
        return try {
            val system = messages.firstOrNull { it.role == MChatRole.System }
                ?.content?.firstOrNull { it.partType == MPartType.TEXT }?.text
            val anthropicMessages = messages
                .filter { it.role != MChatRole.System }
                .map { msg ->
                    val blocks = (msg.content ?: emptyList()).mapNotNull { part ->
                        when (part.partType) {
                            MPartType.TEXT -> part.text?.let { AnthropicContentBlock.text(it) }
                            MPartType.IMAGE -> part.inlineData?.let { dataUrl ->
                                parseDataUrlToImageBlock(dataUrl)
                            }
                            else -> null
                        }
                    }
                    AnthropicMessage(msg.role.toAnthropicRole(), blocks)
                }
            val request = AnthropicMessageRequest(
                model = modelId,
                maxTokens = parameters.tokens ?: 1024,
                messages = anthropicMessages,
                system = system,
                temperature = parameters.variation.temperature,
                topP = parameters.variation.topP,
                topK = parameters.variation.topK,
                stopSequences = parameters.stop?.takeIf { it.isNotEmpty() }
            )
            val resp = client.createMessage(request)
            val errorMsg = resp.error?.message
            if (errorMsg != null) {
                AiPromptTrace.error(modelInfo, "Anthropic error: $errorMsg", duration = System.currentTimeMillis() - t0)
            } else {
                val responseText = resp.firstText() ?: ""
                val responseMsg = TextChatMessage(MChatRole.Assistant, responseText)
                AiTaskTrace(
                    env = AiEnvInfo.of(modelInfo),
                    exec = AiExecInfo.durationSince(t0),
                    output = AiOutputInfo.messages(listOf(responseMsg))
                )
            }
        } catch (x: Exception) {
            AiPromptTrace.error(modelInfo, x.message ?: "Unknown error", duration = System.currentTimeMillis() - t0)
        }
    }

    override fun close() { }

    private fun parseDataUrlToImageBlock(dataUrl: String): AnthropicContentBlock? {
        if (!dataUrl.startsWith("data:") || !dataUrl.contains(";base64,")) return null
        val mediaType = dataUrl.substringBefore(";base64,").substringAfter("data:")
        val base64Data = dataUrl.substringAfter(";base64,")
        return AnthropicContentBlock.image(mediaType, base64Data)
    }
}
