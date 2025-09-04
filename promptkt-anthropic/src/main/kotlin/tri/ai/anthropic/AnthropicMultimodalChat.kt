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

import tri.ai.core.MChatParameters
import tri.ai.core.MChatVariation
import tri.ai.core.MultimodalChat
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.VisionLanguageChatMessage
import tri.ai.anthropic.AnthropicClient.Companion.fromAnthropicRole
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace

/** Multimodal chat with Anthropic Claude models that support vision. */
class AnthropicMultimodalChat(override val modelId: String, val client: AnthropicClient = AnthropicClient.INSTANCE) :
    MultimodalChat {

    override fun toString() = "$modelId (Anthropic Multimodal)"

    override suspend fun chat(
        messages: List<MultimodalChatMessage>,
        parameters: MChatParameters
    ): AiPromptTrace {
        val modelInfo = AiModelInfo.info(modelId, tokens = parameters.tokens, stop = parameters.stop, numResponses = parameters.numResponses)
        val t0 = System.currentTimeMillis()
        
        // Convert multimodal messages to vision language format for images
        val visionMessages = messages.map { msg ->
            val textContent = msg.content?.firstOrNull { it.text != null }?.text ?: ""
            val imageData = msg.content?.firstOrNull { it.inlineData != null }?.inlineData
            VisionLanguageChatMessage(
                role = msg.role,
                content = textContent,
                image = imageData?.let { 
                    try { 
                        java.net.URI(it) 
                    } catch (e: Exception) { 
                        java.net.URI("") 
                    } 
                } ?: java.net.URI("")
            )
        }
        
        // Extract system message
        val systemMessage = visionMessages.firstOrNull { it.role.name.equals("System", ignoreCase = true) }?.content
        val conversationMessages = visionMessages.filter { !it.role.name.equals("System", ignoreCase = true) }
        
        val resp = client.createVisionMessage(
            model = modelId,
            messages = conversationMessages,
            systemMessage = systemMessage,
            maxTokens = parameters.tokens ?: 1024,
            variation = parameters.variation
        )
        return resp.trace(modelInfo, t0)
    }

    override fun close() {
        // No resources to close for this implementation
    }

    companion object {
        /** Create trace for multimodal chat response, with given model info and start query time. */
        internal fun CreateMessageResponse.trace(modelInfo: AiModelInfo, t0: Long): AiPromptTrace {
            return try {
                val role = role.fromAnthropicRole()
                val textContent = content.filterIsInstance<AnthropicContent.Text>()
                val msgs = textContent.map { MultimodalChatMessage.text(role, it.text) }
                
                AiPromptTrace(
                    null,
                    modelInfo,
                    AiExecInfo(responseTimeMillis = System.currentTimeMillis() - t0),
                    AiOutputInfo.multimodalMessages(msgs)
                )
            } catch (e: Exception) {
                AiPromptTrace.error(modelInfo, "Error processing Anthropic multimodal response: ${e.message}", e, duration = System.currentTimeMillis() - t0)
            }
        }
    }

}