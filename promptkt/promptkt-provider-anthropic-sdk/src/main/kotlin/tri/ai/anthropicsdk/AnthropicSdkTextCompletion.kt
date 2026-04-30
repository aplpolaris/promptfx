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

/** Anthropic text completion model using the official SDK. */
class AnthropicSdkTextCompletion(
    override val modelId: String,
    private val client: AnthropicSdkClient
) : TextCompletion {

    override val modelSource = AnthropicSdkModelIndex.MODEL_SOURCE

    override fun toString() = modelDisplayName()

    override suspend fun complete(
        text: String,
        variation: MChatVariation,
        tokens: Int?,
        stop: List<String>?,
        numResponses: Int?
    ): AiPromptTrace {
        val modelInfo = AiModelInfo.info(modelId, tokens = tokens, stop = stop)
        val t0 = System.currentTimeMillis()

        return try {
            val message = MultimodalChatMessage.text(MChatRole.User, text)
            val response = client.createMessage(
                modelId = modelId,
                messages = listOf(message),
                maxTokens = tokens ?: 1024,
                variation = variation,
                stop = stop
            )
            val responseText = response.extractText()
            AiTaskTrace(
                env = AiEnvInfo.of(modelInfo),
                exec = AiExecInfo.durationSince(
                    t0,
                    queryTokens = response.usage().inputTokens().toInt(),
                    responseTokens = response.usage().outputTokens().toInt()
                ),
                output = AiOutputInfo.text(responseText)
            )
        } catch (e: Exception) {
            AiPromptTrace.error(modelInfo, e.message ?: "Unknown error", duration = System.currentTimeMillis() - t0)
        }
    }

}
