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

import com.aallam.openai.api.chat.*
import com.aallam.openai.api.core.Role
import com.aallam.openai.api.model.ModelId
import tri.ai.core.TextChatMessage
import tri.ai.gemini.*
import tri.ai.gemini.Content
import tri.ai.gemini.GeminiClient.Companion.toGeminiRole
import tri.ai.openai.OpenAiChat
import tri.ai.openai.OpenAiClient.Companion.fromOpenAiRole
import tri.ai.openai.OpenAiClient.Companion.toOpenAiRole
import tri.ai.pips.AiPipelineResult
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutputInfo
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

        val m = model.value
        if (m is OpenAiChat) {
            val completion = ChatCompletionRequest(
                model = ModelId(m.modelId),
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
            return m.client.chat(completion).asPipelineResult()
        } else if (m is GeminiTextChat) {
            val response = m.client.generateContent(
                m.modelId,
                GenerateContentRequest(
                    messages.filter { it.role in setOf(ChatRole.User, ChatRole.Assistant) }.map { it.fromOpenAiMessageToGeminiContent() },
                    systemInstruction = messages.firstOrNull { it.role == ChatRole.System }?.geminiSystemMessage(),
                    generationConfig = GenerationConfig(
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
            return if (response.promptFeedback != null)
                AiPromptTrace.invalidRequest<ChatMessage>(m.modelId, response.promptFeedback.toString()).asPipelineResult()
            else
                AiPromptTrace(
                    null,
                    AiModelInfo(m.modelId),
                    AiExecInfo(),
                    AiOutputInfo.output(response.candidates!!.first().content.toChatMessage())
                ).asPipelineResult()
        } else if (m != null) {
            return m.chat(
                messages = messages.map { it.fromOpenAiMessage() },
                tokens = common.maxTokens.value,
                stop = if (common.stopSequences.value.isBlank()) null else listOf(common.stopSequences.value),
                requestJson = responseFormat.value == ChatResponseFormat.JsonObject,
                numResponses = common.numResponses.value
            ).mapOutput {
                it.toOpenAiMessage()
            }.asPipelineResult()
        } else {
            return AiPromptTrace.invalidRequest<ChatMessage>(null, "No model selected in the Chat API view.").asPipelineResult()
        }
    }

    //region API ADAPTERS AND CONVERSIONS

    private fun ChatMessage.fromOpenAiMessage(): TextChatMessage {
        val mc = messageContent
        val usePrompt = when {
            mc is TextContent -> mc.content
            mc is ListContent && mc.content.size == 1 && mc.content[0] is TextPart -> (mc.content[0] as TextPart).text
            else -> null
        }
        return TextChatMessage(role.fromOpenAiRole(), usePrompt)
    }

    private fun TextChatMessage.toOpenAiMessage() =
        ChatMessage(role.toOpenAiRole(), content)

    private fun ChatMessage.geminiSystemMessage() =
        Content.text(content!!)


    private fun ChatMessage.fromOpenAiMessageToGeminiContent() =
        Content(
            role = role.fromOpenAiRole().toGeminiRole(),
            parts = when (val m = messageContent) {
                is TextContent -> listOf(Part(m.content))
                is ListContent -> m.content.map {
                    when (it) {
                        is TextPart -> Part(it.text)
                        is ImagePart -> Part(null, Blob.fromDataUrl(it.imageUrl.url))
                        else -> throw UnsupportedOperationException("Unsupported content type: $it")
                    }
                }
                else -> throw UnsupportedOperationException("Unsupported content type: $m")
            }
        )

    //endregion

}

