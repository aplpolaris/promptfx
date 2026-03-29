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

import tri.ai.anthropic.AnthropicModelIndex.CLAUDE_SONNET_4_6
import tri.ai.core.MChatVariation
import tri.ai.core.TextCompletion
import tri.ai.prompt.trace.AiEnvInfo
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiTaskTrace

/** Text completion with Anthropic Claude models, implemented via the Messages API. */
class AnthropicTextCompletion(
    override val modelId: String = CLAUDE_SONNET_4_6,
    val client: AnthropicClient = AnthropicClient.INSTANCE
) : TextCompletion {

    override val modelSource = AnthropicModelIndex.MODEL_SOURCE

    override fun toString() = modelDisplayName()

    override suspend fun complete(
        text: String,
        variation: MChatVariation,
        tokens: Int?,
        stop: List<String>?,
        numResponses: Int?
    ): AiPromptTrace {
        val modelInfo = AiModelInfo.info(modelId, tokens = tokens, stop = stop, numResponses = numResponses)
        val t0 = System.currentTimeMillis()
        return try {
            val request = AnthropicMessageRequest(
                model = modelId,
                maxTokens = tokens ?: 1024,
                messages = listOf(AnthropicMessage.text("user", text)),
                temperature = variation.temperature,
                topP = variation.topP,
                topK = variation.topK,
                stopSequences = stop?.takeIf { it.isNotEmpty() }
            )
            val resp = client.createMessage(request)
            val errorMsg = resp.error?.message
            if (errorMsg != null) {
                AiPromptTrace.error(modelInfo, "Anthropic error: $errorMsg", duration = System.currentTimeMillis() - t0)
            } else {
                AiTaskTrace(
                    env = AiEnvInfo.of(modelInfo),
                    exec = AiExecInfo.durationSince(t0),
                    output = AiOutputInfo.text(resp.allText())
                )
            }
        } catch (x: Exception) {
            AiPromptTrace.error(modelInfo, x.message ?: "Unknown error", duration = System.currentTimeMillis() - t0)
        }
    }
}
