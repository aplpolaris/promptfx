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
import tri.ai.anthropicsdk.AnthropicSdkClient.Companion.extractText
import tri.ai.prompt.trace.*

/** Anthropic text chat model using the official SDK. */
class AnthropicSdkTextChat(
    override val modelId: String,
    private val client: AnthropicSdkClient
) : TextChat {

    override val modelSource = AnthropicSdkModelIndex.MODEL_SOURCE

    override fun toString() = modelDisplayName()

    override suspend fun chat(
        messages: List<TextChatMessage>,
        variation: MChatVariation,
        tokens: Int?,
        stop: List<String>?,
        numResponses: Int?,
        requestJson: Boolean?
    ): AiPromptTrace {
        val modelInfo = AiModelInfo.info(modelId, tokens = tokens, stop = stop, requestJson = requestJson)
        val t0 = System.currentTimeMillis()

        return try {
            val multimodalMessages = messages.map { MultimodalChatMessage.text(it.role, it.content!!) }
            val response = client.createMessage(
                modelId = modelId,
                messages = multimodalMessages,
                maxTokens = tokens ?: 1024,
                variation = variation,
                stop = stop
            )
            val text = response.extractText()
            AiTaskTrace(
                env = AiEnvInfo.of(modelInfo),
                exec = AiExecInfo.durationSince(
                    t0,
                    queryTokens = response.usage().inputTokens().toInt(),
                    responseTokens = response.usage().outputTokens().toInt()
                ),
                output = AiOutputInfo.text(text)
            )
        } catch (e: Exception) {
            AiPromptTrace.error(modelInfo, e.message ?: "Unknown error", duration = System.currentTimeMillis() - t0)
        }
    }

}
