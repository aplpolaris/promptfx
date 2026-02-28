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

import tri.ai.core.VisionLanguageChat
import tri.ai.core.VisionLanguageChatMessage
import tri.ai.gemini.GeminiModelIndex.GEMINI_25_FLASH_LITE
import tri.ai.gemini.GeminiTextChat.Companion.trace
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiPromptTrace

/** Vision chat completion with Gemini models. */
class GeminiVisionLanguageChat(override val modelId: String = GEMINI_25_FLASH_LITE, val client: GeminiClient = GeminiClient.INSTANCE) :
    VisionLanguageChat {

    override fun toString() = "$modelId (Gemini)"

    override suspend fun chat(
        messages: List<VisionLanguageChatMessage>,
        temp: Double?,
        tokens: Int?,
        stop: List<String>?,
        requestJson: Boolean?
    ): AiPromptTrace {
        val modelInfo = AiModelInfo.info(modelId, tokens = tokens, stop = stop, requestJson = requestJson)
        val t0 = System.currentTimeMillis()
        val resp = client.generateContentVision(messages, modelId,
            GenerationConfig(
                maxOutputTokens = tokens ?: 500,
                stopSequences = stop,
                responseMimeType = if (requestJson == true) "application/json" else null
            )
        )
        return resp.trace(modelInfo, t0)
    }

}
