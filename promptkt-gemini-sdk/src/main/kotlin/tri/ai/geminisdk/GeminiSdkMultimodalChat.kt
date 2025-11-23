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
package tri.ai.geminisdk

import tri.ai.core.*
import tri.ai.prompt.trace.*

/** Gemini multimodal chat model using the official SDK. */
class GeminiSdkMultimodalChat(
    override val modelId: String,
    private val client: GeminiSdkClient
) : MultimodalChat {

    override suspend fun chat(
        messages: List<MultimodalChatMessage>,
        parameters: MChatParameters
    ): AiPromptTrace {
        val modelInfo = AiModelInfo.info(modelId, tokens = parameters.tokens, stop = parameters.stop)
        val t0 = System.currentTimeMillis()
        
        // Extract text content from multimodal messages
        val textMessages = messages.map { msg ->
            val text = msg.content?.firstOrNull()?.text ?: ""
            TextChatMessage(msg.role, text)
        }
        
        val lastMessage = textMessages.lastOrNull() ?: 
            return AiPromptTrace.error(modelInfo, "No messages provided", duration = System.currentTimeMillis() - t0)
        
        val history = textMessages.dropLast(1)
        
        try {
            val response = client.generateContent(
                lastMessage.content ?: "",
                modelId,
                parameters.variation,
                history
            )
            
            val responseText = response.candidatesList.firstOrNull()?.content?.partsList?.firstOrNull()?.text ?: ""
            val responseMsg = MultimodalChatMessage(
                MChatRole.Assistant,
                listOf(MChatMessagePart.text(responseText))
            )
            
            return AiPromptTrace(
                null,
                modelInfo,
                AiExecInfo(responseTimeMillis = System.currentTimeMillis() - t0),
                AiOutputInfo.multimodalMessage(responseMsg)
            )
        } catch (e: Exception) {
            return AiPromptTrace.error(modelInfo, e.message ?: "Unknown error", duration = System.currentTimeMillis() - t0)
        }
    }

    override fun close() {
        client.close()
    }

    override fun toString() = modelId

}
