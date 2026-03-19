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
package tri.ai.geminisdk

import tri.ai.core.SpeechToTextModel
import tri.ai.prompt.trace.*
import java.io.File

/** Speech-to-text with Gemini SDK models. */
class GeminiSdkSpeechToText(
    override val modelId: String = GeminiSdkModelIndex.GEMINI_25_FLASH_LITE,
    private val client: GeminiSdkClient = GeminiSdkClient.INSTANCE
) : SpeechToTextModel {

    override val modelSource = GeminiSdkModelIndex.MODEL_SOURCE

    override fun toString() = modelDisplayName()

    override suspend fun transcribe(audioFile: File, prompt: String?): AiPromptTrace {
        val transcribePrompt = prompt ?: DEFAULT_TRANSCRIBE_PROMPT
        val mimeType = audioFile.mimeType()
        val t0 = System.currentTimeMillis()
        return try {
            val response = client.generateContentAudio(modelId, audioFile.readBytes(), mimeType, transcribePrompt)
            val text = response.text() ?: ""
            AiPromptTrace(
                PromptInfo(transcribePrompt),
                AiModelInfo(modelId),
                AiExecInfo.durationSince(t0),
                AiOutputInfo.text(text)
            )
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
