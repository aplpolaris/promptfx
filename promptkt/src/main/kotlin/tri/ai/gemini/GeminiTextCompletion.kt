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
package tri.ai.gemini

import tri.ai.core.TextChatMessage
import tri.ai.core.TextCompletion
import tri.ai.gemini.GeminiModelIndex.GEMINI_15_FLASH
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace

/** Text completion with Gemini models. */
class GeminiTextCompletion(override val modelId: String = GEMINI_15_FLASH, val client: GeminiClient = GeminiClient.INSTANCE) :
    TextCompletion {

    override fun toString() = "$modelId (Gemini)"

    override suspend fun complete(text: String, tokens: Int?, temperature: Double?, stop: String?, numResponses: Int?, history: List<TextChatMessage>): AiPromptTrace<String> {
        val modelInfo = AiModelInfo.info(modelId, tokens = tokens, stop = stop?.let { listOf(it) }, numResponses = numResponses)
        val t0 = System.currentTimeMillis()
        val resp = client.generateContent(text, modelId, numResponses, history)
        return resp.trace(modelInfo, t0)
    }

    companion object {
        /** Create trace for chat message response, with given model info and start query time. */
        internal fun GenerateContentResponse.trace(modelInfo: AiModelInfo, t0: Long): AiPromptTrace<String> {
            val pf = promptFeedback
            return if (pf?.blockReason != null) {
                val msg = "Gemini blocked response: ${pf.blockReason}"
                AiPromptTrace.error(modelInfo, msg, duration = System.currentTimeMillis() - t0)
            } else if (candidates.isNullOrEmpty()) {
                AiPromptTrace.error(modelInfo, "Gemini returned no candidates", duration = System.currentTimeMillis() - t0)
            } else {
                val respText = candidates?.flatMap { it.content.parts.map { it.text } }?.filterNotNull()
                return AiPromptTrace(
                    null,
                    modelInfo,
                    AiExecInfo(responseTimeMillis = System.currentTimeMillis() - t0),
                    AiOutputInfo(respText ?: listOf())
                )
            }
        }
    }

}

