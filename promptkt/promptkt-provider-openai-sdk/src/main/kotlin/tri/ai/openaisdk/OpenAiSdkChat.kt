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

import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.chat.completions.ChatCompletionMessageParam
import com.openai.models.chat.completions.ChatCompletionUserMessageParam
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tri.ai.core.MChatRole
import tri.ai.core.MChatVariation
import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.prompt.trace.*

/** Chat completion using the OpenAI official Java SDK. */
class OpenAiSdkChat(
    override val modelId: String = OpenAiSdkModelIndex.GPT4O,
    override val modelSource: String = OpenAiSdkModelIndex.MODEL_SOURCE,
    val client: OpenAiSdkClient = OpenAiSdkClient.INSTANCE
) : TextChat {

    override fun toString() = modelDisplayName()

    override suspend fun chat(
        messages: List<TextChatMessage>,
        variation: MChatVariation,
        tokens: Int?,
        stop: List<String>?,
        numResponses: Int?,
        requestJson: Boolean?
    ): AiPromptTrace = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()
        val sdkMessages = messages.map { it.toSdkMessage() }
        val paramsBuilder = ChatCompletionCreateParams.builder()
            .model(modelId)
            .messages(sdkMessages)
            .maxTokens((tokens ?: 500).toLong())
        variation.temperature?.let { paramsBuilder.temperature(it) }
        variation.topP?.let { paramsBuilder.topP(it) }
        stop?.takeIf { it.isNotEmpty() }?.let { paramsBuilder.stop(ChatCompletionCreateParams.Stop.ofStrings(it)) }
        numResponses?.let { paramsBuilder.n(it.toLong()) }
        if (requestJson == true) {
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
            AiModelInfo.MAX_TOKENS to tokens,
            AiModelInfo.TEMPERATURE to variation.temperature,
            AiModelInfo.TOP_P to variation.topP,
            AiModelInfo.NUM_RESPONSES to numResponses
        )
        val usage = response.usage().map { u -> u.totalTokens() }.orElse(null)
        val promptTokens = response.usage().map { u -> u.promptTokens() }.orElse(null)
        val completionTokens = response.usage().map { u -> u.completionTokens() }.orElse(null)

        AiTaskTrace(
            env = AiEnvInfo.of(modelInfo),
            exec = AiExecInfo.durationSince(t0, queryTokens = promptTokens?.toInt(), responseTokens = completionTokens?.toInt()),
            output = AiOutputInfo.messages(choices.map {
                TextChatMessage(MChatRole.Assistant, it.message().content().orElse(""))
            })
        )
    }

}

/** Convert [TextChatMessage] to an SDK [ChatCompletionMessageParam]. */
internal fun TextChatMessage.toSdkMessage(): ChatCompletionMessageParam = when (role) {
    MChatRole.User -> ChatCompletionMessageParam.ofUser(
        ChatCompletionUserMessageParam.builder().content(content ?: "").build()
    )
    MChatRole.Assistant -> ChatCompletionMessageParam.ofAssistant(
        ChatCompletionAssistantMessageParam.builder().content(content ?: "").build()
    )
    MChatRole.System -> ChatCompletionMessageParam.ofSystem(
        ChatCompletionSystemMessageParam.builder().content(content ?: "").build()
    )
    else -> ChatCompletionMessageParam.ofUser(
        ChatCompletionUserMessageParam.builder().content(content ?: "").build()
    )
}
