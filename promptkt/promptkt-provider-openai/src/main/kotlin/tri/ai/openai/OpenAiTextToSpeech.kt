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

import com.aallam.openai.api.audio.SpeechRequest
import com.aallam.openai.api.audio.Voice
import com.aallam.openai.api.model.ModelId
import tri.ai.core.TextToSpeechModel
import tri.ai.prompt.trace.AiPromptTrace

/** Text-to-speech with OpenAI models. */
class OpenAiTextToSpeech(
    override val modelId: String = TTS_1,
    override val modelSource: String = OpenAiModelIndex.MODEL_SOURCE,
    val client: OpenAiAdapter = OpenAiAdapter.INSTANCE
) : TextToSpeechModel {

    override fun toString() = modelDisplayName()

    override suspend fun speech(text: String, voice: String?, speed: Double?): AiPromptTrace =
        client.speech(SpeechRequest(
            model = ModelId(modelId),
            input = text,
            voice = voice?.let { Voice(it) } ?: Voice.Companion.Alloy,
            speed = speed
        )) as AiPromptTrace

    companion object {
        const val TTS_1 = "tts-1"
    }

}
