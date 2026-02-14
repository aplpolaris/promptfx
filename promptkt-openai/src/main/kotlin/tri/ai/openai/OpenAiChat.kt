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
package tri.ai.openai

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatResponseFormat
import com.aallam.openai.api.model.ModelId
import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.core.MChatRole
import tri.ai.core.MChatVariation
import tri.ai.openai.OpenAiModelIndex.GPT35_TURBO
import tri.ai.prompt.trace.AiOutput
import tri.ai.prompt.trace.AiPromptTrace

/** Chat completion with OpenAI models. */
class OpenAiChat(override val modelId: String = GPT35_TURBO, val client: OpenAiAdapter = OpenAiAdapter.INSTANCE) : TextChat {

    override fun toString() = modelId

    override suspend fun chat(
        messages: List<TextChatMessage>,
        variation: MChatVariation,
        tokens: Int?,
        stop: List<String>?,
        numResponses: Int?,
        requestJson: Boolean?
    ): AiPromptTrace =
        client.chatCompletion(ChatCompletionRequest(
            model = ModelId(modelId),
            messages = messages.map { it.toOpenAiMessage() },
            seed = variation.seed,
            temperature = variation.temperature,
            topP = variation.topP,
            presencePenalty = variation.presencePenalty,
            frequencyPenalty = variation.frequencyPenalty,
            maxTokens = tokens ?: 500,
            stop = stop,
            n = numResponses,
            responseFormat = if (requestJson == true) ChatResponseFormat.JsonObject else null
        )).mapOutput { AiOutput(message = TextChatMessage(MChatRole.Assistant, it.text!!)) }

}
