package tri.ai.core

import tri.ai.pips.AiTaskResult

/** Interface for chat completion. */
interface TextChat {

    val modelId: String

    /** Completes user text. */
    suspend fun chat(
        messages: List<TextChatMessage>,
        tokens: Int? = 1000
    ): AiTaskResult<TextChatMessage>

}

/** A single message in a chat. */
class TextChatMessage(val role: TextChatRole, val content: String)

/** The role of a chat message. */
enum class TextChatRole {
    System, User, Assistant
}