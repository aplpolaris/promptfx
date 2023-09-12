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
import tri.ai.core.TextCompletion
import tri.ai.pips.AiTaskResult

/** Text completion with OpenAI chat models. */
class OpenAiCompletionChat(override val modelId: String = COMBO_GPT35, val client: OpenAiClient = OpenAiClient.INSTANCE) :
    TextCompletion {

    override fun toString() = modelId

    override suspend fun complete(text: String, tokens: Int?, temperature: Double?, stop: String?): AiTaskResult<String> =
        client.chatCompletion(ChatCompletionRequest(
            ModelId(modelId),
            listOf(ChatMessage(ChatRole.User, text)),
            temperature = temperature,
            maxTokens = tokens,
            stop = stop?.let { listOf(it) }
        ))

}
