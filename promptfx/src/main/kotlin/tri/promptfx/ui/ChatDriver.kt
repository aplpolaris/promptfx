package tri.promptfx.ui

import tornadofx.Component
import tornadofx.ScopedInstance
import tri.ai.core.TextChatMessage
import tri.ai.core.TextChatRole
import tri.ai.openai.COMBO_GPT35
import tri.ai.openai.OpenAiChat
import tri.ai.openai.OpenAiClient

/** General-purpose tool that generates responses to chat messages. */
abstract class ChatDriver : ScopedInstance, Component() {

    /** Generate a response based on a sequence of prior messages. */
    abstract suspend fun chat(messages: List<ChatEntry>): ChatEntry

}

/** Driver based on OpenAI's GPT API. */
class OpenAiChatDriver : ChatDriver() {

    private val inst = OpenAiClient.INSTANCE
    private val chatter = OpenAiChat(COMBO_GPT35, inst)

    override suspend fun chat(messages: List<ChatEntry>): ChatEntry {
        val response = chatter.chat(messages.mapNotNull { it.toTextChatMessage() })
        return ChatEntry("System", response.value?.content ?: "No response",
            response.value?.role?.toChatRoleStyle() ?: ChatRoleStyle.ERROR)
    }

    //region CONVERSIONS

    private fun ChatEntry.toTextChatMessage() = style.toTextChatRole()?.let {
        TextChatMessage(it, message)
    }

    private fun ChatRoleStyle.toTextChatRole() = when (this) {
        ChatRoleStyle.USER -> TextChatRole.User
        ChatRoleStyle.ASSISTANT -> TextChatRole.Assistant
        ChatRoleStyle.SYSTEM -> TextChatRole.System
        ChatRoleStyle.ERROR -> null
    }

    private fun TextChatRole.toChatRoleStyle(): ChatRoleStyle = when (this) {
        TextChatRole.User -> ChatRoleStyle.USER
        TextChatRole.Assistant -> ChatRoleStyle.ASSISTANT
        TextChatRole.System -> ChatRoleStyle.SYSTEM
    }

    //endregion

}
