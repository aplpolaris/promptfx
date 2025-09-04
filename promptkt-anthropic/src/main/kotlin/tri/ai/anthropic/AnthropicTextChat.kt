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

import tri.ai.core.MChatVariation
import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.anthropic.AnthropicClient.Companion.fromAnthropicRole
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace

/** Text chat with Anthropic Claude models. */
class AnthropicTextChat(override val modelId: String, val client: AnthropicClient = AnthropicClient.INSTANCE) :
    TextChat {

    override fun toString() = "$modelId (Anthropic)"

    override suspend fun chat(
        messages: List<TextChatMessage>,
        variation: MChatVariation,
        tokens: Int?,
        stop: List<String>?,
        numResponses: Int?,
        requestJson: Boolean?
    ): AiPromptTrace {
        val modelInfo = AiModelInfo.info(modelId, tokens = tokens, stop = stop, requestJson = requestJson, numResponses = numResponses)
        val t0 = System.currentTimeMillis()
        
        // Extract system message
        val systemMessage = messages.firstOrNull { it.role.name.equals("System", ignoreCase = true) }?.content
        val conversationMessages = messages.filter { !it.role.name.equals("System", ignoreCase = true) }
        
        val resp = client.createMessage(
            model = modelId,
            messages = conversationMessages,
            systemMessage = systemMessage,
            maxTokens = tokens ?: 1024,
            variation = variation
        )
        return resp.trace(modelInfo, t0)
    }

    companion object {
        /** Create trace for chat message response, with given model info and start query time. */
        internal fun CreateMessageResponse.trace(modelInfo: AiModelInfo, t0: Long): AiPromptTrace {
            return try {
                val role = role.fromAnthropicRole()
                val textContent = content.filterIsInstance<AnthropicContent.Text>()
                val msgs = textContent.map { TextChatMessage(role, it.text) }
                
                AiPromptTrace(
                    null,
                    modelInfo,
                    AiExecInfo(responseTimeMillis = System.currentTimeMillis() - t0),
                    AiOutputInfo.messages(msgs)
                )
            } catch (e: Exception) {
                AiPromptTrace.error(modelInfo, "Error processing Anthropic response: ${e.message}", e, duration = System.currentTimeMillis() - t0)
            }
        }
    }

}