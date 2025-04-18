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
package tri.ai.openai

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatResponseFormat
import com.aallam.openai.api.model.ModelId
import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.core.MChatRole
import tri.ai.openai.OpenAiModelIndex.GPT35_TURBO

/** Chat completion with OpenAI models. */
class OpenAiChat(override val modelId: String = GPT35_TURBO, val client: OpenAiAdapter = OpenAiAdapter.INSTANCE) : TextChat {

    override fun toString() = modelId

    override suspend fun chat(messages: List<TextChatMessage>, tokens: Int?, stop: List<String>?, requestJson: Boolean?, numResponses: Int?) =
        client.chatCompletion(ChatCompletionRequest(
            ModelId(modelId),
            messages.map { it.toOpenAiMessage() },
            maxTokens = tokens ?: 500,
            stop = stop,
            responseFormat = if (requestJson == true) ChatResponseFormat.JsonObject else null,
            n = numResponses
        )).mapOutput { TextChatMessage(MChatRole.Assistant, it) }

}
