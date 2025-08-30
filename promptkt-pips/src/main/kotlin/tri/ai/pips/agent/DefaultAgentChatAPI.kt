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

import tri.ai.core.MultimodalChatMessage

/**
 * Default implementation of [AgentChatAPI] using composition.
 * Combines a session manager and chat implementation for simplified code structure.
 */
class DefaultAgentChatAPI(
    private val sessionManager: AgentSessionManager = DefaultAgentSessionManager(),
    private val chat: AgentChat = DefaultAgentChat()
) : AgentChatAPI {
    
    // Delegate session management to sessionManager
    override fun createSession(config: AgentChatConfig): AgentChatSession = 
        sessionManager.createSession(config)
    
    override fun loadSession(sessionId: String): AgentChatSession? = 
        sessionManager.loadSession(sessionId)
    
    override fun listSessions(): List<AgentChatSessionInfo> = 
        sessionManager.listSessions()
    
    override fun saveSession(session: AgentChatSession): String = 
        sessionManager.saveSession(session)
    
    override fun deleteSession(sessionId: String): Boolean = 
        sessionManager.deleteSession(sessionId)
    
    // Delegate chat functionality to chat
    override fun sendMessage(session: AgentChatSession, message: MultimodalChatMessage): AgentChatOperation = 
        chat.sendMessage(session, message)
    
    override fun addMessage(session: AgentChatSession, message: MultimodalChatMessage) = 
        chat.addMessage(session, message)
    
    override fun getSessionState(session: AgentChatSession): AgentChatSessionState = 
        chat.getSessionState(session)
}