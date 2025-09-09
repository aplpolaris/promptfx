package tri.ai.core.agent

/** Current state of a chat session. */
data class AgentChatSessionState(
    /** The session this state represents. */
    val sessionId: String,
    /** Whether the agent is currently processing. */
    val isProcessing: Boolean = false,
    /** Any error message. */
    val error: String? = null
)