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

import tri.ai.core.MChatRole
import tri.ai.core.MultimodalChatMessage
import java.time.LocalDateTime
import java.util.*

/** Represents a chat session with an agent. */
data class AgentChatSession(
    /** Unique identifier for the session. */
    val sessionId: String = UUID.randomUUID().toString(),
    /** Display name for the session. */
    var name: String = "New Chat",
    /** Configuration for this session. */
    var config: AgentChatConfig = AgentChatConfig(),
    /** Creation timestamp. */
    val createdAt: LocalDateTime = LocalDateTime.now(),
    /** Last modified timestamp. */
    var lastModified: LocalDateTime = LocalDateTime.now(),
    /** Message history for this session. */
    val messages: MutableList<MultimodalChatMessage> = mutableListOf()
) {
    /** Get messages in context based on config limits. */
    fun getContextMessages(): List<MultimodalChatMessage> {
        val systemMsg = config.systemMessage?.let { 
            MultimodalChatMessage.text(MChatRole.System, it) 
        }
        val recentMessages = messages.takeLast(config.maxContextMessages)
        return listOfNotNull(systemMsg) + recentMessages
    }

    /** Create session info for listing purposes. */
    fun toSessionInfo(): AgentChatSessionInfo {
        return AgentChatSessionInfo(
            sessionId = sessionId,
            name = name,
            createdAt = createdAt,
            lastModified = lastModified,
            messageCount = messages.size,
            lastMessagePreview = messages.lastOrNull()?.let { msg ->
                msg.content?.firstOrNull()?.text?.take(50)
            }
        )
    }
}

/** Current state of a chat session. */
data class AgentChatSessionState(
    /** The session this state represents. */
    val session: AgentChatSession,
    /** Whether the agent is currently processing. */
    val isProcessing: Boolean = false,
    /** Any error message. */
    val error: String? = null
)

/** Brief info about a chat session for listing purposes. */
data class AgentChatSessionInfo(
    /** Session ID. */
    val sessionId: String,
    /** Display name. */
    val name: String,
    /** Creation timestamp. */
    val createdAt: LocalDateTime,
    /** Last modified timestamp. */
    val lastModified: LocalDateTime,
    /** Number of messages in the session. */
    val messageCount: Int,
    /** Preview of the last message. */
    val lastMessagePreview: String? = null
)