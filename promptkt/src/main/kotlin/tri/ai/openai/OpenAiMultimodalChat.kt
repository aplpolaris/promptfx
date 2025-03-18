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

import com.aallam.openai.api.chat.*
import com.aallam.openai.api.model.ModelId
import tri.ai.core.*
import tri.ai.openai.OpenAiClient.Companion.fromOpenAiRole
import tri.ai.prompt.trace.AiPromptTrace

/** Chat completion with OpenAI models. */
class OpenAiMultimodalChat(override val modelId: String = OpenAiModelIndex.GPT35_TURBO, val client: OpenAiClient = OpenAiClient.INSTANCE) :
    MultimodalChat {

    override fun toString() = modelId

    override suspend fun chat(
        messages: List<MultimodalChatMessage>,
        parameters: MChatParameters
    ): AiPromptTrace<MultimodalChatMessage> {
        val request = chatCompletionRequest(modelId, messages, parameters)
        val response = client.chat(request)
        return response.mapOutput { it.toMultimodalChatMessage() }
    }

    companion object {
        private const val DEFAULT_MAX_TOKENS = 500

        //region TYPE CONVERSIONS

        private fun chatCompletionRequest(modelId: String, messages: List<MultimodalChatMessage>, parameters: MChatParameters) = ChatCompletionRequest(
            model = ModelId(modelId),
            messages = messages.map { it.openAi() },
            seed = parameters.variation.seed,
            temperature = parameters.variation.temperature,
            topP = parameters.variation.topP,
            frequencyPenalty = parameters.variation.frequencyPenalty,
            presencePenalty = parameters.variation.presencePenalty,
            maxTokens = parameters.tokens ?: DEFAULT_MAX_TOKENS,
            stop = parameters.stop,
            responseFormat = parameters.responseFormat.openAi(),
            n = parameters.numResponses,
            toolChoice = parameters.tools?.toolChoice?.openAi(),
            tools = parameters.tools?.tools?.map { it.openAi() }
        )

        private fun ChatMessage.toMultimodalChatMessage() = MultimodalChatMessage(
            role = role.fromOpenAiRole(),
            content = messageContent?.fromOpenAiContent(),
            toolCalls = toolCalls?.map { (it as ToolCall.Function).fromOpenAi() },
            toolCallId = toolCallId?.id
        )

        private fun MResponseFormat.openAi() = when (this) {
            MResponseFormat.JSON -> ChatResponseFormat.JsonObject
            else -> null
        }

        private fun MultimodalChatMessage.openAi(): ChatMessage {
            return ChatMessageBuilder().apply {
                role = this@openAi.role.openAi()
                content {
                    this@openAi.content?.forEach {
                        if (it.text != null)
                            text(it.text)
                        if (it.inlineData != null)
                            // TODO - validation of data type for this call ??
                            image(it.inlineData)
                        // TODO - API support for additional modalities when available
                    }
                }
                if (!this@openAi.toolCalls.isNullOrEmpty()) {
                    toolCalls = this@openAi.toolCalls.map { it.openAi() }
                }
                toolCallId = this@openAi.toolCallId?.let { ToolId(it) }
            }.build()
        }

        private fun Content.fromOpenAiContent(): List<MChatMessagePart> {
            return when (this) {
                is TextContent -> listOf(MChatMessagePart(text = content))
                is ListContent -> content.map {
                    MChatMessagePart(
                        partType = if (it is TextPart) MPartType.TEXT else if (it is ImagePart) MPartType.IMAGE else throw IllegalStateException(),
                        text = (it as? TextPart)?.text,
                        inlineData = (it as? ImagePart)?.imageUrl?.url
                    )
                }
            }
        }

        private fun MChatRole.openAi() = when (this) {
            MChatRole.User -> ChatRole.User
            MChatRole.System -> ChatRole.System
            MChatRole.Assistant -> ChatRole.Assistant
            MChatRole.Tool -> ChatRole.Tool
        }

        private fun MToolCall.openAi() = ToolCall.Function(
            id = ToolId(id),
            function = FunctionCall(name, argumentsAsJson)
        )

        private fun ToolCall.Function.fromOpenAi() = MToolCall(
            id = id.id,
            name = function.name,
            argumentsAsJson = function.arguments
        )

        private fun MToolChoice.openAi() = when (this) {
            MToolChoice.AUTO -> ToolChoice.Auto
            MToolChoice.NONE -> ToolChoice.None
            is MToolChoice.Named -> ToolChoice.Named(type.openAi(), function.openAi())
            else -> throw IllegalStateException()
        }

        private fun MToolType.openAi() = when (this) {
            MToolType.FUNCTION -> ToolType.Function
            else -> throw IllegalStateException()
        }

        private fun MFunctionToolChoice.openAi() =
            FunctionToolChoice(name)

        private fun MTool.openAi() = Tool.function(
            name = name,
            description = description,
            parameters = Parameters.fromJsonString(jsonSchema)
        )

        //endregion
    }

}
