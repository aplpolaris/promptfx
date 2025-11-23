/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package tri.ai.core.agent

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import tri.ai.core.MultimodalChat
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.TextChat
import tri.ai.core.TextPlugin
import tri.ai.core.textContent
import java.time.LocalDateTime

/** Partial implementation of [AgentChat] with unimplemented methods. */
abstract class AgentChatSupport : AgentChat {

    override fun addMessage(session: AgentChatSession, message: MultimodalChatMessage) {
        session.messages.add(message)
        session.lastModified = LocalDateTime.now()
    }

    override fun sendMessage(session: AgentChatSession, message: MultimodalChatMessage): AgentChatFlow {
        return AgentChatFlow(
            flow {
                val agentResponse = sendMessageSafe(session, message)
                emit(AgentChatEvent.Response(agentResponse))
            }.catch { e ->
                // Remove the user message if we failed to process it
                if (session.messages.lastOrNull() == message) {
                    session.messages.removeAt(session.messages.size - 1)
                }
                tri.util.warning<AgentChatSupport>("Agent chat message failed: ${e.message}", e)
                emit(AgentChatEvent.Error(e))
            })
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

    /** Gets text content from message, with logging. */
    protected suspend fun FlowCollector<AgentChatEvent>.logTextContent(message: MultimodalChatMessage): String {
        emit(AgentChatEvent.Progress("Processing message..."))
        val content = message.textContent() ?: throw IllegalArgumentException("Expected a text message as input.")
        emit(AgentChatEvent.User(content))
        return content
    }

    /** Helper function to add message to session. */
    protected fun updateSession(message: MultimodalChatMessage, session: AgentChatSession, updateName: Boolean = false) {
        session.messages.add(message)
        session.lastModified = LocalDateTime.now()
        if (updateName && session.messages.size == 2 && session.name == "New Chat") {
            session.name = generateSessionName(message)
        }
    }

    /** Looks up a multimodal model for a given session. */
    protected suspend fun findMultimodalChat(session: AgentChatSession, collector: FlowCollector<AgentChatEvent>): MultimodalChat {
        collector.emit(AgentChatEvent.Progress("Finding model..."))
        return try {
            TextPlugin.Companion.multimodalModel(session.config.modelId)
        } catch (e: Exception) {
            val first = TextPlugin.Companion.multimodalModels().first()
            collector.emit(AgentChatEvent.Error(NullPointerException("Model ${session.config.modelId} not found, defaulting to first available model ${first.modelId}.")))
            first
        }
    }

    /** Looks up a model for a given session. */
    protected suspend fun findChat(session: AgentChatSession, collector: FlowCollector<AgentChatEvent>): TextChat {
        collector.emit(AgentChatEvent.Progress("Finding model..."))
        return TextPlugin.Companion.chatModel(session.config.modelId)
    }

    /** Generate a session name from the first user message. */
    private fun generateSessionName(message: MultimodalChatMessage): String {
        val text = message.content?.firstOrNull()?.text ?: "Chat"
        val words = text.split("\\s+".toRegex()).take(5)
        val name = words.joinToString(" ")
        return if (name.length > 30) name.take(27) + "..." else name
    }

    /** Returns response as [AgentChatResponse]. */
    protected fun agentChatResponse(message: MultimodalChatMessage, session: AgentChatSession) =
        AgentChatResponse(message, null, metadata = mapOf("model" to session.config.modelId))

    //endregion

}

