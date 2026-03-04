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
package tri.ai.openai

import tri.ai.core.SpeechToTextModel
import tri.ai.prompt.trace.AiPromptTrace
import java.io.File

/** Speech-to-text model using OpenAI's transcription API. */
class OpenAiSpeechToText(
    override val modelId: String = OpenAiModelIndex.AUDIO_WHISPER,
    override val modelSource: String = OpenAiModelIndex.MODEL_SOURCE,
    val client: OpenAiAdapter = OpenAiAdapter.INSTANCE
) : SpeechToTextModel {

    override fun toString() = modelDisplayName()

    override suspend fun transcribe(audio: File, prompt: String?, language: String?): AiPromptTrace =
        client.quickTranscribe(modelId, audio, prompt, language)

}
