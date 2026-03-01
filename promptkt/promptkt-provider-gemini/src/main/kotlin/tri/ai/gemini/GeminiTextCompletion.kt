/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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

import tri.ai.core.MChatVariation
import tri.ai.core.TextCompletion
import tri.ai.gemini.GeminiModelIndex.GEMINI_25_FLASH_LITE
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace

/** Text completion with Gemini models. */
class GeminiTextCompletion(override val modelId: String = GEMINI_25_FLASH_LITE, val client: GeminiClient = GeminiClient.INSTANCE) :
    TextCompletion {

    override val modelSource = GeminiModelIndex.MODEL_SOURCE

    override fun toString() = modelDisplayName()

    override suspend fun complete(
        text: String,
        variation: MChatVariation,
        tokens: Int?,
        stop: List<String>?,
        numResponses: Int?
    ): AiPromptTrace {
        val modelInfo = AiModelInfo.info(modelId, tokens = tokens, stop = stop, numResponses = numResponses)
        val t0 = System.currentTimeMillis()
        val resp = client.generateContent(text, modelId, variation, numResponses, listOf())
        return resp.trace(modelInfo, t0)
    }

    companion object {
        /** Create trace for chat message response, with given model info and start query time. */
        internal fun GenerateContentResponse.trace(modelInfo: AiModelInfo, t0: Long): AiPromptTrace {
            val pf = promptFeedback
            return if (pf?.blockReason != null) {
                val msg = "Gemini blocked response: ${pf.blockReason}"
                AiPromptTrace.error(modelInfo, msg, duration = System.currentTimeMillis() - t0)
            } else if (candidates.isNullOrEmpty()) {
                AiPromptTrace.error(modelInfo, "Gemini returned no candidates", duration = System.currentTimeMillis() - t0)
            } else {
                // Gemini API appears to return all candidates as parts of content of the first candidate response as well as across multiple candidates, so flatmap doesn't work
                // val respText = candidates?.flatMap { it.content.parts.map { it.text } }?.filterNotNull()
                val respText = candidates?.mapNotNull { it.content.parts.firstOrNull { it.text != null }?.text }
                return AiPromptTrace(
                    null,
                    modelInfo,
                    AiExecInfo(responseTimeMillis = System.currentTimeMillis() - t0),
                    AiOutputInfo.text(respText ?: listOf())
                )
            }
        }
    }

}

