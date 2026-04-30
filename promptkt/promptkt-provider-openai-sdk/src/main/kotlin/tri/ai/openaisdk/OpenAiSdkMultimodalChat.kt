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
package tri.ai.openaisdk

import com.openai.models.chat.completions.ChatCompletionContentPart
import com.openai.models.chat.completions.ChatCompletionContentPartImage
import com.openai.models.chat.completions.ChatCompletionContentPartText
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.chat.completions.ChatCompletionMessageParam
import com.openai.models.chat.completions.ChatCompletionUserMessageParam
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tri.ai.core.*
import tri.ai.prompt.trace.*

/** Multimodal chat using the OpenAI official Java SDK. */
class OpenAiSdkMultimodalChat(
    override val modelId: String = OpenAiSdkModelIndex.GPT4O,
    override val modelSource: String = OpenAiSdkModelIndex.MODEL_SOURCE,
    val client: OpenAiSdkClient = OpenAiSdkClient.INSTANCE
) : MultimodalChat {

    override fun toString() = modelDisplayName()

    override fun close() {
        client.close()
    }

    override suspend fun chat(
        messages: List<MultimodalChatMessage>,
        parameters: MChatParameters
    ): AiPromptTrace = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()
        val sdkMessages = messages.map { it.toSdkMessage() }
        val paramsBuilder = ChatCompletionCreateParams.builder()
            .model(modelId)
            .messages(sdkMessages)
            .maxTokens((parameters.tokens ?: 500).toLong())
        parameters.variation.temperature?.let { paramsBuilder.temperature(it) }
        parameters.variation.topP?.let { paramsBuilder.topP(it) }
        parameters.stop?.takeIf { it.isNotEmpty() }?.let {
            paramsBuilder.stop(ChatCompletionCreateParams.Stop.ofStrings(it))
        }
        parameters.numResponses?.let { paramsBuilder.n(it.toLong()) }
        if (parameters.responseFormat == MResponseFormat.JSON) {
            paramsBuilder.responseFormat(
                ChatCompletionCreateParams.ResponseFormat.ofJsonObject(
                    com.openai.models.ResponseFormatJsonObject.builder().build()
                )
            )
        }

        val response = client.getClient().chat().completions().create(paramsBuilder.build())
        val choices = response.choices()

        val modelInfo = AiModelInfo.info(
            modelId,
            AiModelInfo.MAX_TOKENS to parameters.tokens,
            AiModelInfo.TEMPERATURE to parameters.variation.temperature,
            AiModelInfo.TOP_P to parameters.variation.topP
        )
        val promptTokens = response.usage().map { it.promptTokens() }.orElse(null)
        val completionTokens = response.usage().map { it.completionTokens() }.orElse(null)

        AiTaskTrace(
            env = AiEnvInfo.of(modelInfo),
            exec = AiExecInfo.durationSince(t0, queryTokens = promptTokens?.toInt(), responseTokens = completionTokens?.toInt()),
            output = AiOutputInfo.multimodalMessages(choices.map { choice ->
                MultimodalChatMessage(
                    role = MChatRole.Assistant,
                    content = listOf(MChatMessagePart(text = choice.message().content().orElse("")))
                )
            })
        )
    }

    companion object {

        /** Convert a [MultimodalChatMessage] to an SDK [ChatCompletionMessageParam]. */
        fun MultimodalChatMessage.toSdkMessage(): ChatCompletionMessageParam {
            val parts = content ?: emptyList()
            return when (role) {
                MChatRole.System -> {
                    val text = parts.mapNotNull { it.text }.joinToString("\n")
                    ChatCompletionMessageParam.ofSystem(
                        ChatCompletionSystemMessageParam.builder().content(text).build()
                    )
                }
                MChatRole.Assistant -> {
                    val text = parts.mapNotNull { it.text }.joinToString("")
                    ChatCompletionMessageParam.ofAssistant(
                        ChatCompletionAssistantMessageParam.builder().content(text).build()
                    )
                }
                else -> {
                    // User message — may contain text and/or images
                    if (parts.size == 1 && parts.first().partType == MPartType.TEXT) {
                        ChatCompletionMessageParam.ofUser(
                            ChatCompletionUserMessageParam.builder()
                                .content(parts.first().text ?: "")
                                .build()
                        )
                    } else {
                        val contentParts = parts.map { it.toSdkContentPart() }
                        ChatCompletionMessageParam.ofUser(
                            ChatCompletionUserMessageParam.builder()
                                .contentOfArrayOfContentParts(contentParts)
                                .build()
                        )
                    }
                }
            }
        }

        /** Convert an [MChatMessagePart] to a SDK [ChatCompletionContentPart]. */
        fun MChatMessagePart.toSdkContentPart(): ChatCompletionContentPart {
            return when (partType) {
                MPartType.IMAGE -> {
                    val dataUrl = inlineData ?: ""
                    ChatCompletionContentPart.ofImageUrl(
                        ChatCompletionContentPartImage.builder()
                            .imageUrl(
                                ChatCompletionContentPartImage.ImageUrl.builder()
                                    .url(dataUrl)
                                    .build()
                            )
                            .build()
                    )
                }
                else -> {
                    ChatCompletionContentPart.ofText(
                        ChatCompletionContentPartText.builder()
                            .text(text ?: "")
                            .build()
                    )
                }
            }
        }
    }

}
