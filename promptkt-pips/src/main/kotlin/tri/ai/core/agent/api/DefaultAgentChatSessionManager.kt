/*-
 * #%L
 * tri.promptfx:promptkt-pips
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.core.agent.api

import tri.ai.core.agent.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Default implementation of [AgentChatSessionManager] that keeps sessions in memory.
 */
class DefaultAgentChatSessionManager : AgentChatSessionManager {
    
    private val sessions = ConcurrentHashMap<String, AgentChatSession>()
    
    override fun createSession(config: AgentChatConfig): AgentChatSession {
        val session = AgentChatSession(config = config)
        sessions[session.sessionId] = session
        return session
    }
    
    override fun loadSession(sessionId: String): AgentChatSession? {
        return sessions[sessionId]
    }
    
    override fun listSessions(): List<AgentChatSessionInfo> {
        return sessions.values.map { session ->
            session.toSessionInfo()
        }.sortedByDescending { it.lastModified }
    }
    
    override fun saveSession(session: AgentChatSession): String {
        sessions[session.sessionId] = session
        return session.sessionId
    }
    
    override fun deleteSession(sessionId: String): Boolean {
        return sessions.remove(sessionId) != null
    }
}
