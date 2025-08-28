/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.anthropic

import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.core.MChatRole
import tri.ai.core.MChatVariation
import tri.ai.anthropic.AnthropicModelIndex.CLAUDE_3_5_SONNET
import tri.ai.prompt.trace.AiOutput
import tri.ai.prompt.trace.AiPromptTrace

/** Chat completion with Anthropic models. */
class AnthropicChat(override val modelId: String = CLAUDE_3_5_SONNET, val client: AnthropicAdapter = AnthropicAdapter.INSTANCE) : TextChat {

    override fun toString() = modelId

    override suspend fun chat(
        messages: List<TextChatMessage>,
        variation: MChatVariation,
        tokens: Int?,
        stop: List<String>?,
        numResponses: Int?,
        requestJson: Boolean?
    ): AiPromptTrace {
        
        // Separate system messages from user/assistant messages
        val systemMessages = messages.filter { it.role == MChatRole.System }
        val conversationMessages = messages.filter { it.role != MChatRole.System }
        
        // Combine system messages into a single system prompt
        val systemPrompt = if (systemMessages.isNotEmpty()) {
            systemMessages.joinToString("\n") { it.content ?: "" }
        } else null
        
        // Convert messages to Anthropic format
        val anthropicMessages = conversationMessages.map { msg ->
            AnthropicMessage(
                role = when (msg.role) {
                    MChatRole.User -> "user"
                    MChatRole.Assistant -> "assistant"
                    else -> "user" // fallback
                },
                content = listOf(AnthropicContent(type = "text", text = msg.content ?: ""))
            )
        }
        
        val request = AnthropicChatRequest(
            model = modelId,
            messages = anthropicMessages,
            maxTokens = tokens ?: 1024,
            temperature = variation.temperature,
            topP = variation.topP,
            stopSequences = stop,
            system = systemPrompt
        )
        
        return client.chatCompletion(request)
            .mapOutput { AiOutput(message = TextChatMessage(MChatRole.Assistant, it.text!!)) }
    }

}