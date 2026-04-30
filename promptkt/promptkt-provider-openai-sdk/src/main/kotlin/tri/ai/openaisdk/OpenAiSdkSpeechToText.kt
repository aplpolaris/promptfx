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

import com.openai.models.audio.transcriptions.TranscriptionCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tri.ai.core.SpeechToTextModel
import tri.ai.prompt.trace.*
import java.io.File

/** Speech-to-text using the OpenAI official Java SDK. */
class OpenAiSdkSpeechToText(
    override val modelId: String = OpenAiSdkModelIndex.AUDIO_WHISPER,
    override val modelSource: String = OpenAiSdkModelIndex.MODEL_SOURCE,
    val client: OpenAiSdkClient = OpenAiSdkClient.INSTANCE
) : SpeechToTextModel {

    override fun toString() = modelDisplayName()

    override suspend fun transcribe(audioFile: File, prompt: String?): AiPromptTrace =
        withContext(Dispatchers.IO) {
            val t0 = System.currentTimeMillis()
            val paramsBuilder = TranscriptionCreateParams.builder()
                .model(modelId)
                .file(audioFile.toPath())
            prompt?.let { paramsBuilder.language(it) }

            val response = client.getClient().audio().transcriptions().create(paramsBuilder.build())
            val text = response.asTranscription().text()

            AiTaskTrace(
                env = AiEnvInfo.of(AiModelInfo(modelId)),
                exec = AiExecInfo.durationSince(t0),
                output = AiOutputInfo.text(text)
            )
        }

}
