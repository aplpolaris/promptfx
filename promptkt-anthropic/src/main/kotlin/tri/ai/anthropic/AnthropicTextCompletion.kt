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

import tri.ai.core.MChatRole
import tri.ai.core.MChatVariation
import tri.ai.core.TextCompletion
import tri.ai.core.TextChatMessage
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace

/** Text completion with Anthropic Claude models. */
class AnthropicTextCompletion(override val modelId: String, val client: AnthropicClient = AnthropicClient.INSTANCE) :
    TextCompletion {

    override fun toString() = "$modelId (Anthropic)"

    override suspend fun complete(
        text: String,
        variation: MChatVariation,
        tokens: Int?,
        stop: List<String>?,
        numResponses: Int?
    ): AiPromptTrace {
        val modelInfo = AiModelInfo.info(modelId, tokens = tokens, stop = stop, numResponses = numResponses)
        val t0 = System.currentTimeMillis()
        
        // Convert text completion to chat format (Anthropic only supports chat format)
        val messages = listOf(TextChatMessage(MChatRole.User, text))
        
        val resp = client.createMessage(
            model = modelId,
            messages = messages,
            maxTokens = tokens ?: 1024,
            variation = variation
        )
        return resp.trace(modelInfo, t0)
    }

    companion object {
        /** Create trace for completion response, with given model info and start query time. */
        internal fun CreateMessageResponse.trace(modelInfo: AiModelInfo, t0: Long): AiPromptTrace {
            return try {
                val textContent = content.filterIsInstance<AnthropicContent.Text>()
                val respTexts = textContent.map { it.text }
                
                AiPromptTrace(
                    null,
                    modelInfo,
                    AiExecInfo(responseTimeMillis = System.currentTimeMillis() - t0),
                    AiOutputInfo.text(respTexts)
                )
            } catch (e: Exception) {
                AiPromptTrace.error(modelInfo, "Error processing Anthropic completion response: ${e.message}", e, duration = System.currentTimeMillis() - t0)
            }
        }
    }

}