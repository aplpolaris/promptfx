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

import tri.ai.core.VisionLanguageChat
import tri.ai.core.VisionLanguageChatMessage
import tri.ai.core.MChatRole
import tri.ai.core.TextChatMessage
import tri.ai.openai.java.OpenAiJavaModelIndex.GPT_4O
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace

/** Vision language chat with OpenAI models using the official Java SDK. */
class OpenAiJavaVisionLanguageChat(
    override val modelId: String = GPT_4O,
    val client: OpenAiJavaClient = OpenAiJavaClient.INSTANCE
) : VisionLanguageChat {

    override fun toString() = "$modelId (OpenAI Java SDK)"

    override suspend fun chat(
        messages: List<VisionLanguageChatMessage>,
        temp: Double?,
        tokens: Int?,
        stop: List<String>?,
        requestJson: Boolean?
    ): AiPromptTrace {
        val modelInfo = AiModelInfo.info(modelId, tokens = tokens, stop = stop, requestJson = requestJson)
        val t0 = System.currentTimeMillis()

        val paramsBuilder = com.openai.models.chat.completions.ChatCompletionCreateParams.builder()
            .model(modelId)
            .maxCompletionTokens(tokens?.toLong() ?: 500)

        // Add messages (simplified - text only for now, image support TODO)
        messages.forEach { message ->
            paramsBuilder.addUserMessage(message.content)
        }

        temp?.let { paramsBuilder.temperature(it) }
        stop?.let { paramsBuilder.stop(com.openai.models.chat.completions.ChatCompletionCreateParams.Stop.ofStrings(it)) }

        if (requestJson == true) {
            val jsonFormat = com.openai.models.chat.completions.ChatCompletionCreateParams.ResponseFormat.JsonObject.builder()
            paramsBuilder.responseFormat(com.openai.models.chat.completions.ChatCompletionCreateParams.ResponseFormat.ofJsonObject(jsonFormat.build()))
        }

        val completion = client.client.chat().completions().create(paramsBuilder.build())

        val responseMessages = completion.choices().map { choice ->
            val content = choice.message().content().orElse("")
            TextChatMessage(MChatRole.Assistant, content)
        }

        return AiPromptTrace(
            null,
            modelInfo,
            AiExecInfo(
                responseTimeMillis = System.currentTimeMillis() - t0,
                queryTokens = completion.usage().map { it.promptTokens().toInt() }.orElse(null),
                responseTokens = completion.usage().map { it.completionTokens().toInt() }.orElse(null)
            ),
            AiOutputInfo.messages(responseMessages)
        )
    }

}
