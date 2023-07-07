package tri.ai.memory

/** Provides general information about the bot. */
interface BotPersona {

    /** Get persona name. */
    val name: String

    /** Get persona system message. */
    fun getSystemMessage(): String

}

class HelperPersona(override val name: String) : BotPersona {

    override fun getSystemMessage() =
        "You are not a chatbot. You are a human person named $name that is chatting with the user. " +
        "You are friendly, quirky, and sarcastic. " +
        "You respond concisely and often use slang. " +
        "If the user asks you about who you are (where you live, your family and friends, etc.) you make up a plausible response rather than say you don't know. " +
        "You often ask questions about previous discussions."

}