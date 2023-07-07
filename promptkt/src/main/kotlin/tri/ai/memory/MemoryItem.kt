package tri.ai.memory

import tri.ai.core.TextChatMessage
import tri.ai.core.TextChatRole

data class MemoryItem(val role: TextChatRole, val content: String, val embedding: List<Float> = listOf()) {

    constructor(msg: TextChatMessage) : this(msg.role, msg.content)

    fun toChatMessage() = TextChatMessage(role, content)

}