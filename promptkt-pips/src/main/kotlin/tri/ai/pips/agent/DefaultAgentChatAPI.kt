/*-
 * #%L
 * tri.promptfx:promptkt-pips
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
package tri.ai.pips.agent

import tri.ai.core.*
import tri.ai.openai.OpenAiAdapter
import tri.ai.openai.OpenAiMultimodalChat
import tri.ai.openai.OpenAiModelIndex
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Default implementation of [AgentChatAPI] using OpenAI's multimodal chat capabilities.
 * This provides basic chat functionality with context management and optional tool support.
 */
class DefaultAgentChatAPI(
    private val adapter: OpenAiAdapter = OpenAiAdapter.INSTANCE
) : AgentChatAPI {
    
    private val sessions = ConcurrentHashMap<String, AgentChatSession>()
    
    override fun createSession(config: AgentChatConfig): AgentChatSession {
        val session = AgentChatSession(config = config)
        sessions[session.sessionId] = session
        return session
    }
    
    override suspend fun sendMessage(session: AgentChatSession, message: MultimodalChatMessage): AgentChatResponse {
        // Add user message to session
        session.messages.add(message)
        session.lastModified = LocalDateTime.now()
        
        // Create chat instance
        val chat = OpenAiMultimodalChat(session.config.modelId, adapter)
        
        // Get context messages
        val contextMessages = session.getContextMessages()
        
        // Prepare chat parameters
        val params = MChatParameters(
            variation = MChatVariation(
                temperature = session.config.temperature,
                frequencyPenalty = null,
                presencePenalty = null
            ),
            tokens = session.config.maxTokens,
            responseFormat = MResponseFormat.TEXT,
            numResponses = 1
        )
        
        try {
            // Send to OpenAI
            val response = chat.chat(contextMessages, params)
            
            val responseMessage = response.output?.outputs?.firstOrNull()
                ?: throw IllegalStateException("No response from chat API")
            
            // Add response to session
            session.messages.add(responseMessage)
            session.lastModified = LocalDateTime.now()
            
            // Update session name if this is the first exchange
            if (session.messages.size == 2 && session.name == "New Chat") {
                session.name = generateSessionName(message)
            }
            
            return AgentChatResponse(
                message = responseMessage,
                reasoning = null, // TODO: implement reasoning mode
                metadata = mapOf(
                    "model" to session.config.modelId
                )
            )
        } catch (e: Exception) {
            // Remove the user message if we failed to process it
            if (session.messages.lastOrNull() == message) {
                session.messages.removeAt(session.messages.size - 1)
            }
            throw e
        }
    }
    
    override fun addMessage(session: AgentChatSession, message: MultimodalChatMessage) {
        session.messages.add(message)
        session.lastModified = LocalDateTime.now()
    }
    
    override fun getSessionState(session: AgentChatSession): AgentChatSessionState {
        return AgentChatSessionState(session = session)
    }
    
    override fun saveSession(session: AgentChatSession): String {
        sessions[session.sessionId] = session
        return session.sessionId
    }
    
    override fun loadSession(sessionId: String): AgentChatSession? {
        return sessions[sessionId]
    }
    
    override fun listSessions(): List<AgentChatSessionInfo> {
        return sessions.values.map { session ->
            AgentChatSessionInfo(
                sessionId = session.sessionId,
                name = session.name,
                createdAt = session.createdAt,
                lastModified = session.lastModified,
                messageCount = session.messages.size,
                lastMessagePreview = session.messages.lastOrNull()?.let { msg ->
                    msg.content?.firstOrNull()?.text?.take(50)
                }
            )
        }.sortedByDescending { it.lastModified }
    }
    
    override fun deleteSession(sessionId: String): Boolean {
        return sessions.remove(sessionId) != null
    }
    
    /** Generate a session name from the first user message. */
    private fun generateSessionName(message: MultimodalChatMessage): String {
        val text = message.content?.firstOrNull()?.text ?: "Chat"
        val words = text.split("\\s+".toRegex()).take(5)
        val name = words.joinToString(" ")
        return if (name.length > 30) name.take(27) + "..." else name
    }
}