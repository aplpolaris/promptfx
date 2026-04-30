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

import com.openai.models.models.ModelListParams
import kotlinx.coroutines.runBlocking
import tri.ai.core.*
import java.time.Instant
import java.time.ZoneId

/** Plugin registering OpenAI models and services via the official OpenAI Java SDK. */
class OpenAiSdkAiProvider : AiModelProvider {

    val client = OpenAiSdkClient.INSTANCE

    override fun isApiConfigured() = client.isConfigured()

    override fun modelSource() = OpenAiSdkModelIndex.MODEL_SOURCE

    override fun modelInfo() = if (client.isConfigured())
        try {
            runBlocking {
                val page = client.getClient().models().list(ModelListParams.builder().build())
                page.autoPager().toList().map { it.toModelInfo() }
            }
        } catch (x: Exception) {
            x.printStackTrace()
            emptyList()
        }
    else emptyList()

    override fun embeddingModels() =
        models(OpenAiSdkModelIndex.embeddingModels()) { OpenAiSdkEmbeddingModel(it, modelSource(), client) }

    override fun textCompletionModels() =
        models(OpenAiSdkModelIndex.chatModels()) { OpenAiSdkCompletionChat(it, modelSource(), client) }

    override fun chatModels() =
        models(OpenAiSdkModelIndex.chatModels()) { OpenAiSdkChat(it, modelSource(), client) }

    override fun multimodalModels() =
        models(OpenAiSdkModelIndex.multimodalModels()) { OpenAiSdkMultimodalChat(it, modelSource(), client) }

    override fun imageGeneratorModels() =
        models(OpenAiSdkModelIndex.imageGeneratorModels()) { OpenAiSdkImageGenerator(it, modelSource(), client) }

    override fun textToSpeechModels() =
        models(OpenAiSdkModelIndex.ttsModels()) { OpenAiSdkTextToSpeech(it, modelSource(), client) }

    override fun speechToTextModels() =
        models(OpenAiSdkModelIndex.audioModels()) { OpenAiSdkSpeechToText(it, modelSource(), client) }

    override fun close() {
        client.close()
    }

    private fun <T> models(ids: List<String>, factory: (String) -> T): List<T> =
        if (!client.isConfigured()) listOf() else ids.map(factory)

    //region GETTING MODEL INFO (WITH HEURISTICS)

    private fun com.openai.models.models.Model.toModelInfo(): ModelInfo {
        val existing = OpenAiSdkModelIndex.modelInfoIndex[id()]
        val info = existing ?: ModelInfo(id(), guessModelType(id()), modelSource())
        Instant.ofEpochSecond(created()).atZone(ZoneId.systemDefault()).toLocalDate().let {
            info.metadata.created = it
        }
        if (info.type == ModelType.UNKNOWN) {
            info.type = guessModelType(id())
        }
        return info
    }

    private fun guessModelType(id: String) = when {
        "moderation" in id -> ModelType.MODERATION
        "-realtime-" in id -> ModelType.REALTIME_CHAT
        "-audio-" in id -> ModelType.AUDIO_CHAT
        "whisper" in id || "transcrib" in id -> ModelType.SPEECH_TO_TEXT
        "tts" in id -> ModelType.TEXT_TO_SPEECH
        "embedding" in id || "embed" in id -> ModelType.TEXT_EMBEDDING
        "dall-e" in id || "image" in id -> ModelType.IMAGE_GENERATOR
        "gpt-" in id || "o1" in id || "o3" in id || "o4" in id -> ModelType.TEXT_CHAT
        else -> ModelType.UNKNOWN
    }

    //endregion

}
