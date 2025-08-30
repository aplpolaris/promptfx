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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.core.MChatRole
import tri.ai.core.MultimodalChatMessage

class AgentChatSessionManagerTest {

    @Test
    fun testDefaultAgentSessionManager() {
        val sessionManager = DefaultAgentChatSessionManager()
        
        // Test creating session
        val config = AgentChatConfig(modelId = "gpt-4")
        val session = sessionManager.createSession(config)
        assertNotNull(session.sessionId)
        assertEquals("gpt-4", session.config.modelId)
        
        // Test saving and loading
        val sessionId = sessionManager.saveSession(session)
        assertEquals(session.sessionId, sessionId)
        
        val loadedSession = sessionManager.loadSession(sessionId)
        assertNotNull(loadedSession)
        assertEquals(session.sessionId, loadedSession!!.sessionId)
        
        // Test listing
        val sessions = sessionManager.listSessions()
        assertEquals(1, sessions.size)
        assertEquals(session.sessionId, sessions[0].sessionId)
        
        // Test deleting
        assertTrue(sessionManager.deleteSession(sessionId))
        assertNull(sessionManager.loadSession(sessionId))
        assertTrue(sessionManager.listSessions().isEmpty())
    }

    @Test
    fun testDefaultAgentChat() {
        val chat = DefaultAgentChat()
        val session = AgentChatSession(config = AgentChatConfig())
        val message = MultimodalChatMessage.text(MChatRole.User, "Test message")
        
        // Test adding message
        chat.addMessage(session, message)
        assertEquals(1, session.messages.size)
        assertEquals(message, session.messages[0])
        
        // Test getting session state
        val state = chat.getSessionState(session)
        assertEquals(session, state.session)
        assertFalse(state.isProcessing)
        assertNull(state.error)
        
        // Test send message creates operation
        val operation = chat.sendMessage(session, message)
        assertNotNull(operation)
        assertNotNull(operation.events)
    }

    @Test
    fun testCompositionInDefaultAgentChatAPI() {
        val sessionManager = DefaultAgentChatSessionManager()
        val chat = DefaultAgentChat()
        val api = DefaultAgentChatAPI(sessionManager, chat)
        
        // Test that API delegates correctly
        val config = AgentChatConfig(modelId = "test-model")
        val session = api.createSession(config)
        
        // Session should be created via session manager
        assertEquals("test-model", session.config.modelId)
        assertEquals(session, api.loadSession(session.sessionId))
        
        val message = MultimodalChatMessage.text(MChatRole.User, "Test")
        api.addMessage(session, message)
        assertEquals(1, session.messages.size)
        
        val state = api.getSessionState(session)
        assertEquals(session, state.session)
    }
}