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

import com.anthropic.models.messages.*
import tri.ai.core.*
import tri.ai.prompt.trace.*

/** Vision-language chat completion with Anthropic Claude models. */
class AnthropicVisionLanguageChat(
    override val modelId: String,
    private val client: AnthropicClient
) : VisionLanguageChat {

    override suspend fun chat(
        messages: List<VisionLanguageChatMessage>,
        temp: Double?,
        tokens: Int?,
        stop: List<String>?,
        requestJson: Boolean?
    ): AiPromptTrace {
        val modelInfo = AiModelInfo.info(modelId, tokens = tokens, stop = stop, requestJson = requestJson)
        val t0 = System.currentTimeMillis()

        try {
            // Build message parameters - simplified to just use text for now
            val paramsBuilder = MessageCreateParams.builder()
                .model(modelId)
                .maxTokens(tokens?.toLong() ?: 1024)

            temp?.let { paramsBuilder.temperature(it) }

            // Add messages - simplified to just use text content
            messages.forEach { msg ->
                when (msg.role) {
                    MChatRole.User -> {
                        // For now, just send the text content
                        // TODO: Add proper image support using ContentBlockParam.ofImage
                        paramsBuilder.addUserMessage(msg.content)
                    }
                    MChatRole.Assistant -> {
                        paramsBuilder.addAssistantMessage(msg.content)
                    }
                    else -> {} // Ignore other roles
                }
            }

            val response = client.createMessage(paramsBuilder.build())
            val duration = System.currentTimeMillis() - t0

            val textContent = response.content().stream()
                .flatMap { it.text().stream() }
                .map { it.text() }
                .toList()
                .joinToString("")

            return AiPromptTrace(
                promptInfo = null,
                modelInfo = modelInfo,
                execInfo = AiExecInfo(responseTimeMillis = duration),
                outputInfo = AiOutputInfo.text(textContent)
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - t0
            return AiPromptTrace.error(modelInfo, e.message, e, duration)
        }
    }

    override fun toString() = "$modelId (Anthropic)"

}
