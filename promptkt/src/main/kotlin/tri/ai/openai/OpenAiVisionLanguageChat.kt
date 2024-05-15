/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.openai

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatResponseFormat
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.chatCompletionRequest
import com.aallam.openai.api.model.ModelId
import tri.ai.core.TextChatMessage
import tri.ai.core.TextChatRole
import tri.ai.core.VisionLanguageChat
import tri.ai.core.VisionLanguageChatMessage
import tri.ai.pips.AiTaskResult

/** Vision chat completion with OpenAI models. */
class OpenAiVisionLanguageChat(override val modelId: String, val client: OpenAiClient = OpenAiClient.INSTANCE) :
    VisionLanguageChat {

    override fun toString() = modelId

    override suspend fun chat(
        messages: List<VisionLanguageChatMessage>,
        temp: Double?,
        tokens: Int?,
        stop: List<String>?,
        requestJson: Boolean?
    ): AiTaskResult<TextChatMessage> {
        val response = client.chat(
            chatCompletionRequest {
                model = ModelId(modelId)
                temperature = temp
                maxTokens = tokens
                this.stop = stop
                this.messages = messages.map { it.openAiMessage() }
                responseFormat = if (requestJson == true) ChatResponseFormat.JsonObject else null
            }
        )
        return response.map { TextChatMessage(TextChatRole.Assistant, it.content!!) }
    }

    private fun VisionLanguageChatMessage.openAiMessage() = ChatMessage(role.openAiRole(), content)

    private fun TextChatRole.openAiRole() = when (this) {
        TextChatRole.System -> ChatRole.System
        TextChatRole.User -> ChatRole.User
        TextChatRole.Assistant -> ChatRole.Assistant
    }

}
