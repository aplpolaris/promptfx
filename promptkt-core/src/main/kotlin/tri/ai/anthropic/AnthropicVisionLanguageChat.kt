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

import tri.ai.core.*
import tri.ai.anthropic.AnthropicModelIndex.CLAUDE_3_5_SONNET
import tri.ai.prompt.trace.AiOutput
import tri.ai.prompt.trace.AiPromptTrace

/** Vision language chat with Anthropic models. */
class AnthropicVisionLanguageChat(override val modelId: String = CLAUDE_3_5_SONNET, val client: AnthropicAdapter = AnthropicAdapter.INSTANCE) : VisionLanguageChat {

    override fun toString() = modelId

    override suspend fun chat(
        messages: List<VisionLanguageChatMessage>,
        temp: Double?,
        tokens: Int?,
        stop: List<String>?,
        requestJson: Boolean?
    ): AiPromptTrace {
        
        // Convert vision language messages to basic text + image format for now
        val anthropicMessages = messages.map { msg ->
            AnthropicMessage(
                role = when (msg.role) {
                    MChatRole.User -> "user"
                    MChatRole.Assistant -> "assistant"
                    else -> "user"
                },
                content = listOf(AnthropicContent(type = "text", text = msg.content))
            )
        }
        
        val request = AnthropicChatRequest(
            model = modelId,
            messages = anthropicMessages,
            maxTokens = tokens ?: 1024,
            temperature = temp,
            stopSequences = stop
        )
        
        return client.chatCompletion(request)
            .mapOutput { AiOutput(text = it.text) }
    }

}