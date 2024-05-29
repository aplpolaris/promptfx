/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx.api

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.core.Role
import com.aallam.openai.api.model.ModelId
import tri.ai.pips.AiPipelineResult

/**
 * Basic version of chat through API.
 * See https://beta.openai.com/docs/api-reference/chat for more information.
 */
class ChatViewBasic :
    ChatView("Chat", "Testing AI Assistant chat.", listOf(Role.User, Role.Assistant), showInput = false) {

    override suspend fun processUserInput(): AiPipelineResult {
        val systemMessage = if (system.value.isNullOrBlank()) listOf() else
            listOf(ChatMessage(ChatRole.System, system.value))
        val messages = systemMessage + chatHistory.chatMessages().takeLast(messageHistory.value)

        val completion = ChatCompletionRequest(
            model = ModelId(model.value),
            messages = messages,
            temperature = common.temp.value,
            topP = common.topP.value,
            n = null,
            stop = if (common.stopSequences.value.isBlank()) null else common.stopSequences.value.split("||"),
            maxTokens = common.maxTokens.value,
            presencePenalty = common.presPenalty.value,
            frequencyPenalty = common.freqPenalty.value,
            logitBias = null,
            user = null,
            functions = null,
            functionCall = null,
            responseFormat = responseFormat.value,
            tools = null,
            toolChoice = null,
            seed = if (seedActive.value) seed.value else null
        )
        return controller.openAiPlugin.client.chat(completion).asPipelineResult()
    }

}

