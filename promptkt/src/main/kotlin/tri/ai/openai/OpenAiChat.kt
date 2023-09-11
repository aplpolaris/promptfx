/*-
 * #%L
 * promptkt-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.core.TextChatRole

/** Chat completion with OpenAI models. */
class OpenAiChat(override val modelId: String = COMBO_GPT35, val client: OpenAiClient = OpenAiClient.INSTANCE) : TextChat {

    override suspend fun chat(messages: List<TextChatMessage>, tokens: Int?, stop: List<String>?) =
        client.chatCompletion(ChatCompletionRequest(
            ModelId(modelId),
            messages.map { it.openAiMessage() },
            maxTokens = tokens ?: 500,
            stop = stop
        )).map { TextChatMessage(TextChatRole.Assistant, it) }

    private fun TextChatMessage.openAiMessage() = ChatMessage(role.openAiRole(), content)

    private fun TextChatRole.openAiRole() = when (this) {
        TextChatRole.System -> ChatRole.System
        TextChatRole.User -> ChatRole.User
        TextChatRole.Assistant -> ChatRole.Assistant
    }

}
