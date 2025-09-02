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
package tri.ai.core.agent

import kotlinx.coroutines.flow.FlowCollector
import tri.ai.core.*

/**
 * Default implementation of [AgentChat] supporting any multimodal model from the plugin system.
 * This provides streaming chat functionality with context management.
 */
class DefaultAgentChat : BaseAgentChat() {

    override suspend fun FlowCollector<AgentChatEvent>.sendMessageSafe(session: AgentChatSession, message: MultimodalChatMessage): AgentChatResponse {
        // store and log message
        processMessage(session, message, this)
        val chat = findMultimodalChat(session, this)

        // get messages in context and prepare to execute model
        emit(AgentChatEvent.Progress("Gathering context..."))
        val contextMessages = session.messagesInCurrentContext()
        val builder = CompletionBuilder()
            .tokens(session.config.maxTokens)
            .numResponses(1)
            .responseFormat(MResponseFormat.TEXT)
            .temperature(session.config.temperature)

        // generate response
        emit(AgentChatEvent.Progress("Generating response..."))
        val response = builder.execute(chat, contextMessages)
        val responseMessage = response.output?.outputs?.firstOrNull()?.multimodalMessage
            ?: throw IllegalStateException("No response from chat API")

        // store and log response, maybe update session name
        processMessageResponse(session, responseMessage, this)

        return AgentChatResponse(responseMessage, null, metadata = mapOf("model" to session.config.modelId))
    }

}