/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx.api

import tri.ai.core.*
import tri.ai.pips.AiPipelineResult
import tri.ai.pips.asPipelineResult

/**
 * Basic version of chat through API.
 * See https://beta.openai.com/docs/api-reference/chat for more information.
 */
class ChatViewBasic :
    ChatView("Chat", "Testing AI Assistant chat.", listOf(MChatRole.User, MChatRole.Assistant), showInput = false) {

    override suspend fun processUserInput(): AiPipelineResult {
        val systemMessage = if (system.value.isNullOrBlank()) listOf() else
            listOf(MultimodalChatMessage.text(MChatRole.System, system.value))
        val messages = systemMessage + chatHistory.chatMessages().takeLast(messageHistory.value)
        val params = MChatParameters(
            variation = MChatVariation(
                seed = if (seedActive.value) seed.value else null,
                temperature = common.temp.value,
                topP = common.topP.value,
                frequencyPenalty = common.freqPenalty.value,
                presencePenalty = common.presPenalty.value
            ),
            tokens = common.maxTokens.value,
            stop = if (common.stopSequences.value.isBlank()) null else common.stopSequences.value.split("||"),
            responseFormat = responseFormat.value,
            numResponses = common.numResponses.value
        )

        val result = model.value!!.chat(messages, params)
        return result.asPipelineResult()
    }

}

