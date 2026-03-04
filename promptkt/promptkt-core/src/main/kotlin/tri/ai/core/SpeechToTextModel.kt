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
package tri.ai.core

import tri.ai.prompt.trace.AiPromptTrace
import java.io.File

/** Interface for speech-to-text (audio transcription) models. */
interface SpeechToTextModel : AiModel {

    /**
     * Transcribe audio from a file.
     * @param audio the audio file to transcribe
     * @param prompt optional text to guide the model's transcription style or provide context
     * @param language optional language of the audio (ISO-639-1 code, e.g. "en")
     */
    suspend fun transcribe(
        audio: File,
        prompt: String? = null,
        language: String? = null
    ): AiPromptTrace

    companion object {
        val UNAVAILABLE = object : SpeechToTextModel {
            override val modelId = "Unavailable"
            override val modelSource = ""
            override fun toString() = modelDisplayName()
            override suspend fun transcribe(audio: File, prompt: String?, language: String?) =
                AiPromptTrace.error(modelInfo = null, message = "Speech-to-text model is unavailable.")
        }
    }
}
