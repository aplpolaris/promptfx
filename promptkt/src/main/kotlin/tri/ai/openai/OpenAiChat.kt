package tri.ai.openai

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.core.TextChatRole

/** Chat completion with OpenAI models. */
class OpenAiChat(override val modelId: String = COMBO_GPT35, val client: OpenAiClient = OpenAiClient.INSTANCE) : TextChat {

    @OptIn(BetaOpenAI::class)
    override suspend fun chat(messages: List<TextChatMessage>, tokenLimit: Int?) =
        client.chatCompletion(ChatCompletionRequest(
            ModelId(modelId),
            messages.map { it.openAiMessage() },
            maxTokens = tokenLimit ?: 500
        )).map { TextChatMessage(TextChatRole.Assistant, it) }

    @OptIn(BetaOpenAI::class)
    private fun TextChatMessage.openAiMessage() = ChatMessage(role.openAiRole(), content)

    @OptIn(BetaOpenAI::class)
    private fun TextChatRole.openAiRole() = when (this) {
        TextChatRole.System -> ChatRole.System
        TextChatRole.User -> ChatRole.User
        TextChatRole.Assistant -> ChatRole.Assistant
    }

}