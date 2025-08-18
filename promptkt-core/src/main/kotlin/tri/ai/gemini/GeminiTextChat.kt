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

import tri.ai.core.MChatVariation
import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.gemini.GeminiClient.Companion.fromGeminiRole
import tri.ai.gemini.GeminiModelIndex.GEMINI_15_FLASH
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace

/** Text chat with Gemini models. */
class GeminiTextChat(override val modelId: String = GEMINI_15_FLASH, val client: GeminiClient = GeminiClient.INSTANCE) :
    TextChat {

    override fun toString() = "$modelId (Gemini)"

    override suspend fun chat(
        messages: List<TextChatMessage>,
        variation: MChatVariation,
        tokens: Int?,
        stop: List<String>?,
        numResponses: Int?,
        requestJson: Boolean?
    ): AiPromptTrace<TextChatMessage> {
        val modelInfo = AiModelInfo.info(modelId, tokens = tokens, stop = stop, requestJson = requestJson, numResponses = numResponses)
        val t0 = System.currentTimeMillis()
        val resp = client.generateContent(
            messages, modelId,
            GenerationConfig(
                maxOutputTokens = tokens ?: 500,
                stopSequences = stop,
                responseMimeType = if (requestJson == true) "application/json" else null
            )
        )
        return resp.trace(modelInfo, t0)
    }

    companion object {
        /** Create trace for chat message response, with given model info and start query time. */
        internal fun GenerateContentResponse.trace(modelInfo: AiModelInfo, t0: Long): AiPromptTrace<TextChatMessage> {
            val pf = promptFeedback
            return if (pf?.blockReason != null) {
                val msg = "Gemini blocked response: ${pf.blockReason}"
                AiPromptTrace.error(modelInfo, msg, duration = System.currentTimeMillis() - t0)
            } else if (candidates.isNullOrEmpty()) {
                AiPromptTrace.error(modelInfo, "Gemini returned no candidates", duration = System.currentTimeMillis() - t0)
            } else {
                val firstCandidate = candidates!!.first()
                val role = firstCandidate.content.role.fromGeminiRole()
                val msgs = firstCandidate.content.parts.map { TextChatMessage(role, it.text) }
                AiPromptTrace(
                    null,
                    modelInfo,
                    AiExecInfo(responseTimeMillis = System.currentTimeMillis() - t0),
                    AiOutputInfo(msgs)
                )
            }
        }
    }

}
