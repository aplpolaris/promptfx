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

class AgentChatAPITest {

    private val api = DefaultAgentChatAPI()

    @Test
    fun testCreateSession() {
        val config = AgentChatConfig(modelId = "gpt-3.5-turbo")
        val session = api.createSession(config)
        
        assertNotNull(session.sessionId)
        assertEquals("New Chat", session.name)
        assertEquals(config.modelId, session.config.modelId)
        assertTrue(session.messages.isEmpty())
    }

    @Test
    fun testAddMessage() {
        val session = api.createSession()
        val message = MultimodalChatMessage.text(MChatRole.User, "Hello, world!")
        
        api.addMessage(session, message)
        
        assertEquals(1, session.messages.size)
        assertEquals(message, session.messages[0])
    }

    @Test
    fun testGetContextMessages() {
        val config = AgentChatConfig(
            systemMessage = "You are a helpful assistant.",
            maxContextMessages = 3
        )
        val session = api.createSession(config)
        
        // Add some messages
        repeat(5) { i ->
            val message = MultimodalChatMessage.text(MChatRole.User, "Message $i")
            api.addMessage(session, message)
        }
        
        val contextMessages = session.getContextMessages()
        
        // Should include system message + last 3 messages
        assertEquals(4, contextMessages.size)
        assertEquals(MChatRole.System, contextMessages[0].role)
        assertEquals("You are a helpful assistant.", contextMessages[0].content?.get(0)?.text)
        assertEquals("Message 2", contextMessages[1].content?.get(0)?.text)
        assertEquals("Message 4", contextMessages[3].content?.get(0)?.text)
    }

    @Test
    fun testSessionPersistence() {
        val session = api.createSession()
        val message = MultimodalChatMessage.text(MChatRole.User, "Test message")
        api.addMessage(session, message)
        
        val sessionId = api.saveSession(session)
        assertEquals(session.sessionId, sessionId)
        
        val loadedSession = api.loadSession(sessionId)
        assertNotNull(loadedSession)
        assertEquals(session.sessionId, loadedSession!!.sessionId)
        assertEquals(1, loadedSession.messages.size)
        assertEquals("Test message", loadedSession.messages[0].content?.get(0)?.text)
    }

    @Test
    fun testListSessions() {
        val session1 = api.createSession()
        val session2 = api.createSession()
        
        api.addMessage(session1, MultimodalChatMessage.text(MChatRole.User, "Session 1 message"))
        api.addMessage(session2, MultimodalChatMessage.text(MChatRole.User, "Session 2 message"))
        
        val sessionInfos = api.listSessions()
        
        assertEquals(2, sessionInfos.size)
        val sessionIds = sessionInfos.map { it.sessionId }.toSet()
        assertTrue(sessionIds.contains(session1.sessionId))
        assertTrue(sessionIds.contains(session2.sessionId))
    }

    @Test
    fun testDeleteSession() {
        val session = api.createSession()
        val sessionId = api.saveSession(session)
        
        assertTrue(api.deleteSession(sessionId))
        assertFalse(api.deleteSession(sessionId)) // Should return false when already deleted
        assertNull(api.loadSession(sessionId))
    }
}