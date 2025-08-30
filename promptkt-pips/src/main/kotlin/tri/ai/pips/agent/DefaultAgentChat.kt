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
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime

/**
 * Default implementation of [AgentChat] supporting any multimodal model from the plugin system.
 * This provides streaming chat functionality with context management.
 */
class DefaultAgentChat : AgentChat {
    
    override fun sendMessage(session: AgentChatSession, message: MultimodalChatMessage): AgentChatOperation {
        return AgentChatOperation(flow {
            try {
                emit(AgentChatEvent.Progress("Processing message..."))
                
                // Add user message to session
                session.messages.add(message)
                session.lastModified = LocalDateTime.now()
                
                emit(AgentChatEvent.Progress("Finding model..."))
                
                // Get multimodal chat instance for the configured model
                val chat = TextPlugin.multimodalModel(session.config.modelId)
                
                // Get context messages
                val contextMessages = session.getContextMessages()
                
                emit(AgentChatEvent.Progress("Generating response..."))
                
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
                
                // Send to model
                val response = chat.chat(contextMessages, params)
                
                val responseMessage = response.output?.outputs?.firstOrNull()?.multimodalMessage
                    ?: throw IllegalStateException("No response from chat API")
                
                // Add response to session
                session.messages.add(responseMessage)
                session.lastModified = LocalDateTime.now()
                
                // Update session name if this is the first exchange
                if (session.messages.size == 2 && session.name == "New Chat") {
                    session.name = generateSessionName(message)
                }
                
                val agentResponse = AgentChatResponse(
                    message = responseMessage,
                    reasoning = null, // TODO: implement reasoning mode
                    metadata = mapOf(
                        "model" to session.config.modelId
                    )
                )
                
                emit(AgentChatEvent.Response(agentResponse))
                
            } catch (e: Exception) {
                // Remove the user message if we failed to process it
                if (session.messages.lastOrNull() == message) {
                    session.messages.removeAt(session.messages.size - 1)
                }
                emit(AgentChatEvent.Error(e))
            }
        })
    }
    
    override fun addMessage(session: AgentChatSession, message: MultimodalChatMessage) {
        session.messages.add(message)
        session.lastModified = LocalDateTime.now()
    }
    
    override fun getSessionState(session: AgentChatSession): AgentChatSessionState {
        return AgentChatSessionState(session = session)
    }
    
    /** Generate a session name from the first user message. */
    private fun generateSessionName(message: MultimodalChatMessage): String {
        val text = message.content?.firstOrNull()?.text ?: "Chat"
        val words = text.split("\\s+".toRegex()).take(5)
        val name = words.joinToString(" ")
        return if (name.length > 30) name.take(27) + "..." else name
    }
}