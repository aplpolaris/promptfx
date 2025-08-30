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
 * API for managing agent chat sessions with contextual and reasoning capabilities.
 * This API is designed to be independent of UI concerns and can support CLI, MCP, or REST interfaces.
 */
interface AgentChatAPI {

    // sessions

    /** Create a new chat session with optional configuration. */
    fun createSession(config: AgentChatConfig = AgentChatConfig()): AgentChatSession

    /** Load a chat session by ID. */
    fun loadSession(sessionId: String): AgentChatSession?

    /** List available chat sessions. */
    fun listSessions(): List<AgentChatSessionInfo>

    /** Get the current session state including message history. */
    fun getSessionState(session: AgentChatSession): AgentChatSessionState

    /** Save a chat session. */
    fun saveSession(session: AgentChatSession): String

    /** Delete a chat session. */
    fun deleteSession(sessionId: String): Boolean

    // messages
    
    /** Send a message to a chat session and get a streaming operation. */
    fun sendMessage(session: AgentChatSession, message: MultimodalChatMessage): AgentChatOperation
    
    /** Add a message to the session without processing it. */
    fun addMessage(session: AgentChatSession, message: MultimodalChatMessage)

}