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
package tri.ai.openaisdk

import com.openai.models.audio.speech.SpeechCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tri.ai.core.TextToSpeechModel
import tri.ai.prompt.trace.*

/** Text-to-speech using the OpenAI official Java SDK. */
class OpenAiSdkTextToSpeech(
    override val modelId: String = OpenAiSdkModelIndex.TTS_1,
    override val modelSource: String = OpenAiSdkModelIndex.MODEL_SOURCE,
    val client: OpenAiSdkClient = OpenAiSdkClient.INSTANCE
) : TextToSpeechModel {

    override fun toString() = modelDisplayName()

    override suspend fun speech(text: String, voice: String?, speed: Double?): AiPromptTrace =
        withContext(Dispatchers.IO) {
            val t0 = System.currentTimeMillis()
            val paramsBuilder = SpeechCreateParams.builder()
                .model(modelId)
                .input(text)
                .voice(voice ?: DEFAULT_VOICE)
            speed?.let { paramsBuilder.speed(it) }

            val response = client.getClient().audio().speech().create(paramsBuilder.build())
            val audioBytes = response.body().use { it.readBytes() }

            AiTaskTrace(
                env = AiEnvInfo.of(AiModelInfo.info(modelId)),
                exec = AiExecInfo.durationSince(t0),
                output = AiOutputInfo.other(audioBytes)
            )
        }

    companion object {
        const val DEFAULT_VOICE = "alloy"
    }

}
