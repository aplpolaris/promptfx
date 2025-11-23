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
import tri.util.info

/** Multimodal chat completion with Anthropic Claude models. */
class AnthropicMultimodalChat(
    override val modelId: String,
    private val client: AnthropicClient
) : MultimodalChat {

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

        if ((parameters.numResponses ?: 1) > 1)
            info<AnthropicMultimodalChat>("Anthropic chat API does not support multiple responses; only the first response will be returned.")

        try {
            // Build message parameters
            val paramsBuilder = MessageCreateParams.builder()
                .model(modelId)
                .maxTokens(parameters.tokens?.toLong() ?: 1024)

            // Handle system message
            val systemMessage = messages.lastOrNull { it.role == MChatRole.System }
            systemMessage?.let { msg ->
                val textContent = msg.content?.mapNotNull { it.text }?.joinToString(" ") ?: ""
                if (textContent.isNotEmpty()) {
                    paramsBuilder.system(textContent)
                }
            }

            // Add user and assistant messages
            val nonSystemMessages = messages.filter { it.role != MChatRole.System }
            nonSystemMessages.forEach { msg ->
                val textContent = msg.content?.mapNotNull { it.text }?.joinToString(" ") ?: ""
                when (msg.role) {
                    MChatRole.User -> {
                        if (textContent.isNotEmpty()) {
                            paramsBuilder.addUserMessage(textContent)
                        }
                    }
                    MChatRole.Assistant -> {
                        if (textContent.isNotEmpty()) {
                            paramsBuilder.addAssistantMessage(textContent)
                        }
                    }
                    else -> {} // Ignore other roles
                }
            }

            // Apply variation parameters
            val temp: Double? = parameters.variation.temperature
            if (temp != null) {
                paramsBuilder.temperature(temp)
            }
            val topPVal: Double? = parameters.variation.topP
            if (topPVal != null) {
                paramsBuilder.topP(topPVal)
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

    override fun close() {
        client.close()
    }

    override fun toString() = modelId

}
