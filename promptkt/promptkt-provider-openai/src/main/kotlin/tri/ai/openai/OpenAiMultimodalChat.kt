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

import com.aallam.openai.api.chat.*
import com.aallam.openai.api.model.ModelId
import tri.ai.core.*
import tri.ai.prompt.trace.AiPromptTrace

/** Chat completion with OpenAI models. */
class OpenAiMultimodalChat(override val modelId: String = OpenAiModelIndex.GPT35_TURBO, override val modelSource: String = OpenAiModelIndex.MODEL_SOURCE, val client: OpenAiAdapter = OpenAiAdapter.INSTANCE) :
    MultimodalChat {

    override fun toString() = modelDisplayName()

    override suspend fun chat(
        messages: List<MultimodalChatMessage>,
        parameters: MChatParameters
    ): AiPromptTrace {
        val request = chatCompletionRequest(modelId, messages, parameters)
        return client.chat(request, multimodal = true)
    }

    override fun close() {
        client.client.close()
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

        private fun MResponseFormat.openAi() = when (this) {
            MResponseFormat.JSON -> ChatResponseFormat.JsonObject
            else -> null
        }

        fun MultimodalChatMessage.openAi(): ChatMessage {
            return ChatMessageBuilder().apply {
                role = this@openAi.role.openAi()
                content {
                    this@openAi.content?.forEach {
                        val textVal = it.text
                        if (textVal != null)
                            text(textVal)
                        val inlineDataVal = it.inlineData
                        if (inlineDataVal != null)
                            // TODO - validation of data type for this call ??
                            image(inlineDataVal)
                        // TODO - API support for additional modalities when available
                    }
                }
                val toolCallsVal = this@openAi.toolCalls
                if (!toolCallsVal.isNullOrEmpty()) {
                    toolCalls = toolCallsVal.map { it.openAi() }
                }
                toolCallId = this@openAi.toolCallId?.let { ToolId(it) }
            }.build()
        }

        private fun MChatRole.openAi() = when (this) {
            MChatRole.User -> ChatRole.User
            MChatRole.System -> ChatRole.System
            MChatRole.Assistant -> ChatRole.Assistant
            MChatRole.Tool -> ChatRole.Tool
            MChatRole.None -> ChatRole.User // default to user if no role specified
        }

        private fun MToolCall.openAi() = ToolCall.Function(
            id = ToolId(id),
            function = FunctionCall(name, argumentsAsJson)
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
