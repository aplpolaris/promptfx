package tri.ai.core.agent

import tri.ai.core.MultimodalChatMessage

/** Agentic chat interface, supporting messaging and some state management. */
interface AgentChat {
    /** Send a message to a chat session and get a streaming operation. */
    fun sendMessage(session: AgentChatSession, message: MultimodalChatMessage): AgentChatFlow

    /** Add a message to the session without processing it. */
    fun addMessage(session: AgentChatSession, message: MultimodalChatMessage)

    /** Get the current session state including message history. */
    fun getSessionState(session: AgentChatSession): AgentChatSessionState
}

