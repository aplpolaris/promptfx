package tri.ai.core.agent

import kotlinx.coroutines.flow.FlowCollector
import tri.ai.core.MultimodalChat
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.TextChat
import tri.ai.core.TextPlugin
import java.time.LocalDateTime

/** Agentic chat interface, supporting messaging and some state management. */
interface AgentChat {

    /** Send a message to a chat session and get a streaming operation. */
    fun sendMessage(session: AgentChatSession, message: MultimodalChatMessage): AgentChatFlow

    /** Add a message to the session without processing it. */
    fun addMessage(session: AgentChatSession, message: MultimodalChatMessage)

    /** Get the current session state including message history. */
    fun getSessionState(session: AgentChatSession): AgentChatSessionState

}

/** Current state of a chat session. */
data class AgentChatSessionState(
    /** The session this state represents. */
    val sessionId: String,
    /** Whether the agent is currently processing. */
    val isProcessing: Boolean = false,
    /** Any error message. */
    val error: String? = null
)

/** Partial implementation of [AgentChat] with unimplemented methods. */
abstract class BaseAgentChat : AgentChat {

    override fun addMessage(session: AgentChatSession, message: MultimodalChatMessage) {
        session.messages.add(message)
        session.lastModified = LocalDateTime.now()
    }

    override fun sendMessage(session: AgentChatSession, message: MultimodalChatMessage) = agentflow {
        try {
            val agentResponse = sendMessageSafe(session, message)
            emit(AgentChatEvent.Response(agentResponse))
        } catch (e: Exception) {
            // Remove the user message if we failed to process it
            if (session.messages.lastOrNull() == message) {
                session.messages.removeAt(session.messages.size - 1)
            }
            println("sendMessage failed with error: ${e.message}")
            e.printStackTrace()
            emit(AgentChatEvent.Error(e))
        }
    }

    /**
     * Perform send message task while emitting intermediate events.
     * This is called within a try/catch block in [sendMessage] to handle errors.
     * This should return with a final [AgentChatEvent.Response] or [AgentChatEvent.Error].
     */
    abstract suspend fun FlowCollector<AgentChatEvent>.sendMessageSafe(session: AgentChatSession, message: MultimodalChatMessage): AgentChatResponse

    override fun getSessionState(session: AgentChatSession) =
        AgentChatSessionState(sessionId = session.sessionId)

    //region CONVENIENCE METHODS FOR IMPLEMENTATIONS

    /** Helper function to ensure text input from a [MultimodalChatMessage], and throw an exception otherwise. */
    protected fun ensureTextContent(message: MultimodalChatMessage): String {
        return message.content?.getOrNull(0)?.text
            ?: throw IllegalArgumentException("Expected a text message as input.")
    }

    /** Helper function to add message to session. */
    protected suspend fun processMessage(session: AgentChatSession, message: MultimodalChatMessage, collector: FlowCollector<AgentChatEvent>) {
        collector.emit(AgentChatEvent.Progress("Processing message..."))
        session.messages.add(message)
        session.lastModified = LocalDateTime.now()
    }

    /** Looks up a multimodal model for a given session. */
    protected suspend fun findMultimodalChat(session: AgentChatSession, collector: FlowCollector<AgentChatEvent>): MultimodalChat {
        collector.emit(AgentChatEvent.Progress("Finding model..."))
        return try {
            TextPlugin.multimodalModel(session.config.modelId)
        } catch (e: Exception) {
            val first = TextPlugin.multimodalModels().first()
            collector.emit(AgentChatEvent.Error(NullPointerException("Model ${session.config.modelId} not found, defaulting to first available model ${first.modelId}.")))
            first
        }
    }

    /** Looks up a multimodal model for a given session. */
    protected suspend fun findChat(session: AgentChatSession, collector: FlowCollector<AgentChatEvent>): TextChat {
        collector.emit(AgentChatEvent.Progress("Finding model..."))
        return TextPlugin.chatModel(session.config.modelId)
    }

    /** Helper function to add response message to session. */
    protected fun processMessageResponse(session: AgentChatSession, response: MultimodalChatMessage, collector: FlowCollector<AgentChatEvent>) {
        session.messages.add(response)
        session.lastModified = LocalDateTime.now()

        if (session.messages.size == 2 && session.name == "New Chat") {
            session.name = generateSessionName(response)
        }
    }

    /** Generate a session name from the first user message. */
    private fun generateSessionName(message: MultimodalChatMessage): String {
        val text = message.content?.firstOrNull()?.text ?: "Chat"
        val words = text.split("\\s+".toRegex()).take(5)
        val name = words.joinToString(" ")
        return if (name.length > 30) name.take(27) + "..." else name
    }

    //endregion

}