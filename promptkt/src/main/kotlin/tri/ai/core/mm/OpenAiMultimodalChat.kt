package tri.ai.core.mm

import com.aallam.openai.api.chat.*
import com.aallam.openai.api.model.ModelId
import tri.ai.core.TextChatRole
import tri.ai.openai.OpenAiClient
import tri.ai.openai.OpenAiModelIndex
import tri.ai.prompt.trace.AiPromptTrace

/** Chat completion with OpenAI models. */
class OpenAiMultimodalChat(override val modelId: String = OpenAiModelIndex.GPT35_TURBO, val client: OpenAiClient = OpenAiClient.INSTANCE) :
    MultimodalChat {

    override fun toString() = modelId

    override suspend fun chat(
        messages: List<MultimodalChatMessage>,
        parameters: MChatParameters
    ): AiPromptTrace<MultimodalChatMessage> {
        val request = ChatCompletionRequest(
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
        val response = client.chat(request)
        return response.mapOutput { it.toMultimodalChatMessage() }
    }

    companion object {
        private const val DEFAULT_MAX_TOKENS = 500

        //region TYPE CONVERSIONS

        private fun ChatMessage.toMultimodalChatMessage() =
            MultimodalChatMessage.text(TextChatRole.Assistant, content!!)

        private fun MultimodalResponseFormat.openAi() = when (this) {
            MultimodalResponseFormat.JSON -> ChatResponseFormat.JsonObject
            else -> null
        }

        private fun MultimodalChatMessage.openAi(): ChatMessage {
            return ChatMessageBuilder().apply {
                role = this@openAi.role.openAi()
                content {
                    this@openAi.content.forEach {
                        if (it.text != null)
                            text(it.text)
                        if (it.inlineData != null)
                            // TODO - validation of data type for this call ??
                            image(it.inlineData)
                        // TODO - API support for additional modalities when available
                    }
                }
                if (this@openAi.toolCalls.isNotEmpty()) {
                    toolCalls = this@openAi.toolCalls.map { it.openAi() }
                }
            }.build()
        }

        private fun TextChatRole.openAi() = when (this) {
            TextChatRole.User -> ChatRole.User
            TextChatRole.System -> ChatRole.System
            TextChatRole.Assistant -> ChatRole.Assistant
        }

        private fun MToolCall.openAi() = ToolCall.Function(
            id = ToolId(id),
            function = FunctionCall(name, argumentsAsJson)
        )

        private fun MToolChoice.openAi() = when (this) {
            MToolChoice.AUTO -> ToolChoice.Auto
            MToolChoice.NONE -> ToolChoice.None
        }

        private fun MTool.openAi() = com.aallam.openai.api.chat.Tool.function(
            name = name,
            description = description,
            parameters = Parameters.fromJsonString(jsonSchema)
        )

        //endregion
    }

}