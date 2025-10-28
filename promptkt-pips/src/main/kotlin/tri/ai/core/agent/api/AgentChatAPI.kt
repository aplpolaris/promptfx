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
package tri.ai.core.agent.api

import tri.ai.core.agent.AgentChat
import tri.ai.core.agent.DefaultAgentChat

/**
 * API for managing agent chat sessions with contextual and reasoning capabilities.
 * This API is designed to be independent of UI concerns and can support CLI, MCP, or REST interfaces.
 * Combines session management and chat functionality through composition.
 */
interface AgentChatAPI : AgentChatSessionManager, AgentChat

/**
 * Default implementation of [AgentChatAPI] using separate [AgentChatSessionManager] and [AgentChat] implementations.
 */
class DefaultAgentChatAPI(
    private val sessionManager: AgentChatSessionManager = DefaultAgentChatSessionManager(),
    private val chat: AgentChat = DefaultAgentChat()
) : AgentChatAPI, AgentChat by chat, AgentChatSessionManager by sessionManager