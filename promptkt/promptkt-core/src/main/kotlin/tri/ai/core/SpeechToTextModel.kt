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

/** Interface for speech-to-text models. */
interface SpeechToTextModel : AiModel {

    /** Transcribes audio from a file, returning an [AiPromptTrace] with the transcription text in the output. */
    suspend fun transcribe(
        audioFile: File,
        prompt: String? = null
    ): AiPromptTrace

    companion object {
        val UNAVAILABLE = object : SpeechToTextModel {
            override val modelId = "Unavailable"
            override val modelSource = ""
            override fun toString() = modelDisplayName()
            override suspend fun transcribe(audioFile: File, prompt: String?) =
                AiPromptTrace.error(modelInfo = null, message = "Speech-to-text model is unavailable.")
        }
    }

}
