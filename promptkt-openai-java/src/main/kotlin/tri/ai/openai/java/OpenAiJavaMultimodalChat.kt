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
package tri.ai.openai.java

import tri.ai.core.*
import tri.ai.openai.java.OpenAiJavaModelIndex.GPT_4O
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace

/** Multimodal chat with OpenAI models using the official Java SDK. */
class OpenAiJavaMultimodalChat(
    override val modelId: String = GPT_4O,
    val client: OpenAiJavaClient = OpenAiJavaClient.INSTANCE
) : MultimodalChat {

    override fun toString() = "$modelId (OpenAI Java SDK)"

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

        val paramsBuilder = com.openai.models.chat.completions.ChatCompletionCreateParams.builder()
            .model(modelId)
            .messages(messages.map { it.toOpenAiMessage() })
            .maxCompletionTokens(parameters.tokens?.toLong() ?: 500)

        parameters.variation.temperature?.let { paramsBuilder.temperature(it.toDouble()) }
        parameters.variation.topP?.let { paramsBuilder.topP(it.toDouble()) }
        parameters.variation.seed?.let { paramsBuilder.seed(it.toLong()) }
        parameters.variation.presencePenalty?.let { paramsBuilder.presencePenalty(it.toDouble()) }
        parameters.variation.frequencyPenalty?.let { paramsBuilder.frequencyPenalty(it.toDouble()) }

        parameters.stop?.let { paramsBuilder.stop(com.openai.models.chat.completions.ChatCompletionCreateParams.Stop.ofStrings(it)) }
        parameters.numResponses?.let { paramsBuilder.n(it.toLong()) }

        if (parameters.responseFormat == MResponseFormat.JSON) {
            paramsBuilder.responseFormat(
                com.openai.models.chat.completions.ChatCompletionCreateParams.ResponseFormat.ofJsonObject(
                    com.openai.models.chat.completions.ChatCompletionCreateParams.ResponseFormat.JsonObject.builder().build()
                )
            )
        }

        val completion = client.client.chat().completions().create(paramsBuilder.build())

        val responseMessage = completion.choices().firstOrNull()?.let { choice ->
            val content = choice.message().content().orElse("")
            MultimodalChatMessage(
                role = MChatRole.Assistant,
                content = listOf(MChatMessagePart.text(content))
            )
        } ?: MultimodalChatMessage(MChatRole.Assistant, listOf(MChatMessagePart.text("")))

        return AiPromptTrace(
            null,
            modelInfo,
            AiExecInfo(
                responseTimeMillis = System.currentTimeMillis() - t0,
                queryTokens = completion.usage().map { it.promptTokens().toInt() }.orElse(null),
                responseTokens = completion.usage().map { it.completionTokens().toInt() }.orElse(null)
            ),
            AiOutputInfo.multimodalMessage(responseMessage)
        )
    }

    override fun close() {
        client.close()
    }

    private fun MultimodalChatMessage.toOpenAiMessage(): com.openai.models.chat.completions.ChatCompletionMessageParam {
        return when (role) {
            MChatRole.User -> {
                val parts = content?.map { part ->
                    when (part.partType) {
                        MPartType.TEXT -> com.openai.models.chat.completions.ChatCompletionContentPartTextParam.ofText(part.text!!)
                        MPartType.IMAGE -> {
                            // Assume base64 encoded data URL
                            com.openai.models.chat.completions.ChatCompletionContentPartImageParam.ofImageUrl(
                                com.openai.models.chat.completions.ChatCompletionContentPartImageParam.ImageUrl.builder()
                                    .url(part.inlineData!!)
                                    .build()
                            )
                        }
                        else -> throw UnsupportedOperationException("Unsupported part type: ${part.partType}")
                    }
                } ?: emptyList()
                
                com.openai.models.chat.completions.ChatCompletionUserMessageParam.ofArrayOfContentParts(parts)
            }
            MChatRole.System -> {
                val text = content?.firstOrNull()?.text ?: ""
                com.openai.models.chat.completions.ChatCompletionSystemMessageParam.ofText(text)
            }
            MChatRole.Assistant -> {
                val text = content?.firstOrNull()?.text ?: ""
                com.openai.models.chat.completions.ChatCompletionAssistantMessageParam.builder().content(text).build()
            }
            MChatRole.Tool -> throw UnsupportedOperationException("Tool role not yet supported")
            else -> throw UnsupportedOperationException("Unsupported role: $role")
        }
    }

}
