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

import tri.ai.core.MChatRole
import tri.ai.core.MChatVariation
import tri.ai.core.TextChatMessage
import tri.ai.core.TextCompletion
import tri.ai.openai.OpenAiModelIndex.GPT35_TURBO
import tri.ai.prompt.trace.AiPromptTrace

/** Text completion with OpenAI chat models. */
class OpenAiCompletionChat(override val modelId: String = GPT35_TURBO, override val modelSource: String = OpenAiModelIndex.MODEL_SOURCE, val client: OpenAiAdapter = OpenAiAdapter.INSTANCE) :
    TextCompletion {

    override fun toString() = modelDisplayName()

    override suspend fun complete(
        text: String,
        variation: MChatVariation,
        tokens: Int?,
        stop: List<String>?,
        numResponses: Int?
    ): AiPromptTrace = OpenAiChat(modelId, modelSource, client).chat(
        listOf(TextChatMessage(MChatRole.User, text)),
        variation = variation,
        tokens = tokens ?: 1000,
        stop = stop,
        numResponses = numResponses ?: 1,
    )

}
