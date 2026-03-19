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

import tri.ai.core.SpeechToTextModel
import tri.ai.prompt.trace.*
import java.io.File
import java.util.Base64

/** Speech-to-text with Gemini models. */
class GeminiSpeechToText(
    override val modelId: String = GeminiModelIndex.GEMINI_25_FLASH_LITE,
    val client: GeminiClient = GeminiClient.INSTANCE
) : SpeechToTextModel {

    override val modelSource = GeminiModelIndex.MODEL_SOURCE

    override fun toString() = modelDisplayName()

    override suspend fun transcribe(audioFile: File, prompt: String?): AiPromptTrace {
        val transcribePrompt = prompt ?: DEFAULT_TRANSCRIBE_PROMPT
        val mimeType = audioFile.mimeType()
        val base64Audio = Base64.getEncoder().encodeToString(audioFile.readBytes())
        val request = GenerateContentRequest(Content(
            listOf(
                Part.text(transcribePrompt),
                Part(inlineData = Blob(mimeType, base64Audio))
            ), ContentRole.user
        ))
        val t0 = System.currentTimeMillis()
        return try {
            val response = client.generateContent(modelId, request)
            if (response.candidates.isNullOrEmpty()) {
                AiPromptTrace.error(AiModelInfo(modelId), "Gemini returned no candidates", duration = System.currentTimeMillis() - t0)
            } else {
                AiPromptTrace(
                    PromptInfo(transcribePrompt),
                    AiModelInfo(modelId),
                    AiExecInfo.durationSince(t0),
                    AiOutputInfo.text(response.candidates?.get(0)?.content?.parts?.get(0)?.text.orEmpty())
                )
            }
        } catch (e: Exception) {
            AiPromptTrace.error(AiModelInfo(modelId), e.message ?: "Unknown error", duration = System.currentTimeMillis() - t0)
        }
    }

    companion object {
        private const val DEFAULT_TRANSCRIBE_PROMPT = "Transcribe this audio"

        /** Determine MIME type from file extension. */
        private fun File.mimeType(): String = when (extension.lowercase()) {
            "mp3" -> "audio/mp3"
            "wav" -> "audio/wav"
            "mp4" -> "audio/mp4"
            "m4a" -> "audio/m4a"
            "webm" -> "audio/webm"
            "mpeg", "mpga" -> "audio/mpeg"
            else -> "audio/wav"
        }
    }

}
