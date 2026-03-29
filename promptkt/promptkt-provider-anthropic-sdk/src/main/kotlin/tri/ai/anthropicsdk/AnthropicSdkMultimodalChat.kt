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

import tri.ai.core.*
import tri.ai.anthropicsdk.AnthropicSdkClient.Companion.toMChatMessagePart
import tri.ai.prompt.trace.*

/** Anthropic multimodal chat model using the official SDK. */
class AnthropicSdkMultimodalChat(
    override val modelId: String,
    private val client: AnthropicSdkClient
) : MultimodalChat {

    override val modelSource = AnthropicSdkModelIndex.MODEL_SOURCE

    override fun toString() = modelDisplayName()

    override suspend fun chat(
        messages: List<MultimodalChatMessage>,
        parameters: MChatParameters
    ): AiPromptTrace {
        val modelInfo = AiModelInfo.info(
            modelId,
            tokens = parameters.tokens,
            stop = parameters.stop,
            requestJson = parameters.responseFormat == MResponseFormat.JSON
        )
        val t0 = System.currentTimeMillis()

        return try {
            val response = client.createMessage(
                modelId = modelId,
                messages = messages,
                maxTokens = parameters.tokens ?: 1024,
                variation = parameters.variation,
                stop = parameters.stop
            )

            val contentParts = response.content().map { it.toMChatMessagePart() }
            val msg = MultimodalChatMessage(MChatRole.Assistant, contentParts)

            AiTaskTrace(
                env = AiEnvInfo.of(modelInfo),
                exec = AiExecInfo.durationSince(
                    t0,
                    queryTokens = response.usage().inputTokens().toInt(),
                    responseTokens = response.usage().outputTokens().toInt()
                ),
                output = AiOutputInfo.multimodalMessage(msg)
            )
        } catch (e: Exception) {
            AiPromptTrace.error(modelInfo, e.message ?: "Unknown error", duration = System.currentTimeMillis() - t0)
        }
    }

    override fun close() {
        client.close()
    }

}
