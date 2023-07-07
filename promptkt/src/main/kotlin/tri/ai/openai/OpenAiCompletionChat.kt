package tri.ai.openai

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import tri.ai.core.TextCompletion

/** Text completion with OpenAI chat models. */
@OptIn(BetaOpenAI::class)
class OpenAiCompletionChat(val modelId: String = COMBO_GPT35, val client: OpenAiClient = OpenAiClient.INSTANCE) :
    TextCompletion {

    override fun toString() = modelId

    override suspend fun complete(text: String, tokens: Int?, stop: String?) =
        client.chatCompletion(ChatCompletionRequest(
            ModelId(modelId),
            listOf(ChatMessage(ChatRole.User, text)),
            maxTokens = tokens,
            stop = stop?.let { listOf(it) }
        ))

}