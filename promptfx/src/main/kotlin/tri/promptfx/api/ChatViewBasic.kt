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

import com.aallam.openai.api.chat.*
import com.aallam.openai.api.core.Role
import com.aallam.openai.api.model.ModelId
import tri.ai.core.TextPlugin
import tri.ai.gemini.*
import tri.ai.gemini.Content
import tri.ai.openai.OpenAiChat
import tri.ai.pips.AiPipelineResult
import tri.ai.prompt.trace.AiPromptTrace

/**
 * Basic version of chat through API.
 * See https://beta.openai.com/docs/api-reference/chat for more information.
 */
class ChatViewBasic :
    ChatView("Chat", "Testing AI Assistant chat.", listOf(Role.User, Role.Assistant), showInput = false) {

    override suspend fun processUserInput(): AiPipelineResult<ChatMessage> {
        val systemMessage = if (system.value.isNullOrBlank()) listOf() else
            listOf(ChatMessage(ChatRole.System, system.value))
        val messages = systemMessage + chatHistory.chatMessages().takeLast(messageHistory.value)

        val m = TextPlugin.chatModel(model.value)
        if (m is OpenAiChat) {
            val completion = ChatCompletionRequest(
                model = ModelId(model.value),
                messages = messages,
                temperature = common.temp.value,
                topP = common.topP.value,
                n = common.numResponses.value,
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
                seed = if (seedActive.value) seed.value else null,
                logprobs = null,
                topLogprobs = null
            )
            val response = m.client.chat(completion)
            return response.asPipelineResult()
        } else if (m is GeminiTextChat) {
            val response = m.client.generateContent(
                m.modelId,
                GenerateContentRequest(
                    messages.filter { it.role in setOf(ChatRole.User, ChatRole.Assistant) }.map { it.geminiContent() },
                    systemInstruction = messages.firstOrNull { it.role == ChatRole.System }?.geminiSystemMessage(),
                    GenerationConfig(
                        stopSequences = if (common.stopSequences.value.isBlank()) null else common.stopSequences.value.split("||"),
                        responseMimeType = responseFormat.value?.let {
                            when (it) {
                                ChatResponseFormat.Text -> "text/plain"
                                ChatResponseFormat.JsonObject -> "application/json"
                                else -> throw UnsupportedOperationException("Unsupported response format: $it")
                            }
                        },
                        candidateCount = null,
                        maxOutputTokens = common.maxTokens.value,
                        temperature = common.temp.value,
                        topP = common.topP.value,
                        topK = null,
                    )
                )
            )
            return if (response.error != null)
                AiPromptTrace.invalidRequest<ChatMessage>(response.error!!.message).asPipelineResult()
            else
                AiPromptTrace.result(response.candidates!!.first().content.toChatMessage(), m.modelId).asPipelineResult()
        } else {
            return AiPromptTrace.invalidRequest<ChatMessage>("This model/plugin is not supported in the Chat API view: $m").asPipelineResult()
        }
    }

    private fun ChatMessage.geminiSystemMessage() =
        Content.text(content!!)

    private fun ChatMessage.geminiContent() =
        Content(
            role = when (role) {
                ChatRole.User -> "user"
                ChatRole.Assistant -> "model"
                else -> throw UnsupportedOperationException("Unsupported role: $role")
            },
            parts = when (val m = messageContent) {
                is TextContent -> listOf(Part(m.content))
                is ListContent -> m.content.map {
                    when (it) {
                        is TextPart -> Part(it.text)
                        is ImagePart -> Part(null, Blob.image(it.imageUrl.url))
                        else -> throw UnsupportedOperationException("Unsupported content type: $it")
                    }
                }
                else -> throw UnsupportedOperationException("Unsupported content type: $m")
            }
        )

}

