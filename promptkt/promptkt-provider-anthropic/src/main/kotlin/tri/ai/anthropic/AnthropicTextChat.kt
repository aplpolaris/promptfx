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
import tri.ai.core.MChatRole
import tri.ai.core.MChatVariation
import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.prompt.trace.AiEnvInfo
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiTaskTrace

/** Text chat with Anthropic Claude models. */
class AnthropicTextChat(
    override val modelId: String = CLAUDE_SONNET_4_6,
    val client: AnthropicClient = AnthropicClient.INSTANCE
) : TextChat {

    override val modelSource = AnthropicModelIndex.MODEL_SOURCE

    override fun toString() = modelDisplayName()

    override suspend fun chat(
        messages: List<TextChatMessage>,
        variation: MChatVariation,
        tokens: Int?,
        stop: List<String>?,
        numResponses: Int?,
        requestJson: Boolean?
    ): AiPromptTrace {
        val modelInfo = AiModelInfo.info(modelId, tokens = tokens, stop = stop, requestJson = requestJson, numResponses = numResponses)
        val t0 = System.currentTimeMillis()
        return try {
            val system = messages.lastOrNull { it.role == MChatRole.System }?.content
            val anthropicMessages = messages
                .filter { it.role != MChatRole.System }
                .map { AnthropicMessage.text(it.role.toAnthropicRole(), it.content ?: "") }
            val request = AnthropicMessageRequest(
                model = modelId,
                maxTokens = tokens ?: 1024,
                messages = anthropicMessages,
                system = system,
                temperature = variation.temperature,
                topP = variation.topP,
                topK = variation.topK,
                stopSequences = stop?.takeIf { it.isNotEmpty() }
            )
            val resp = client.createMessage(request)
            resp.trace(modelInfo, t0)
        } catch (x: Exception) {
            AiPromptTrace.error(modelInfo, x.message ?: "Unknown error", duration = System.currentTimeMillis() - t0)
        }
    }

    companion object {
        /** Build a trace from an Anthropic response. */
        internal fun AnthropicMessageResponse.trace(modelInfo: AiModelInfo, t0: Long): AiPromptTrace {
            val errorMsg = error?.message
            return if (errorMsg != null) {
                AiPromptTrace.error(modelInfo, "Anthropic error: $errorMsg", duration = System.currentTimeMillis() - t0)
            } else {
                val responseText = firstText() ?: ""
                val responseMsg = TextChatMessage(MChatRole.Assistant, responseText)
                AiTaskTrace(
                    env = AiEnvInfo.of(modelInfo),
                    exec = AiExecInfo.durationSince(t0),
                    output = AiOutputInfo.messages(listOf(responseMsg))
                )
            }
        }
    }
}
