package tri.promptfx.ui

/** A message from a user. */
class ChatEntry(val user: String, val message: String, val style: ChatRoleStyle = ChatRoleStyle.USER) {
    override fun toString() = "$user: $message"
}