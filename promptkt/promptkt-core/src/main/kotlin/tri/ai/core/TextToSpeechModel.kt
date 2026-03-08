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

/** Interface for text-to-speech models. */
interface TextToSpeechModel : AiModel {

    /** Generates speech from text, returning an [AiPromptTrace] with audio content as [ByteArray] in the output. */
    suspend fun speech(
        text: String,
        voice: String? = null,
        speed: Double? = null
    ): AiPromptTrace

    companion object {
        val UNAVAILABLE = object : TextToSpeechModel {
            override val modelId = "Unavailable"
            override val modelSource = ""
            override fun toString() = modelDisplayName()
            override suspend fun speech(text: String, voice: String?, speed: Double?) =
                AiPromptTrace.error(modelInfo = null, message = "Text-to-speech model is unavailable.")
        }
    }

}
