/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
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

import com.aallam.openai.api.model.Model
import kotlinx.coroutines.runBlocking
import tri.ai.core.DataModality
import tri.ai.core.ModelInfo
import tri.ai.core.ModelType
import tri.ai.core.TextPlugin
import tri.util.fine
import java.time.Instant
import java.time.ZoneId

/**
 * Implementation of [TextPlugin] using OpenAI API.
 * Models are as described in `openai-models.yaml`.
 */
class OpenAiPlugin : TextPlugin {

    val client = OpenAiClient.INSTANCE

    override fun modelSource() = "OpenAI"

    override fun modelInfo() = try {
        runBlocking {
            client.client.models().map { it.toModelInfo() }
        }
    } catch (x: Exception) {
        x.printStackTrace()
        emptyList()
    }

    private fun Model.toModelInfo(): ModelInfo {
        val existing = OpenAiModelIndex.MODEL_INFO_INDEX[id.id]
        val info = existing ?: ModelInfo(id.id, ModelType.UNKNOWN, modelSource())
        created?.let {
            info.created = Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }

        if (info.type == ModelType.UNKNOWN) {
            when {
                "moderation" in id.id -> info.type = ModelType.MODERATION
                "-realtime-" in id.id -> {
                    info.type = ModelType.REALTIME_CHAT
                    info.inputs = listOf(DataModality.text, DataModality.audio)
                    info.outputs = listOf(DataModality.text, DataModality.audio)
                }
                "-audio-" in id.id -> {
                    info.type = ModelType.AUDIO_CHAT
                    info.inputs = listOf(DataModality.audio)
                    info.outputs = listOf(DataModality.audio)
                }
                else -> {
                    // attempt to assign type for tagged models based on a "parent type"
                    var possibleTypeId = id.id
                    while (OpenAiModelIndex.MODEL_INFO_INDEX[possibleTypeId]?.type in listOf(null, ModelType.UNKNOWN)) {
                        possibleTypeId = possibleTypeId.substringBeforeLast("-")
                        if ("-" !in possibleTypeId) break
                    }
                    info.type = OpenAiModelIndex.MODEL_INFO_INDEX[possibleTypeId]?.type ?: ModelType.UNKNOWN
                }
            }
        }

        return info
    }

    override fun embeddingModels() =
        OpenAiModelIndex.embeddingModels().map { OpenAiEmbeddingService(it, client) }

    override fun textCompletionModels() =
        OpenAiModelIndex.chatModelsInclusive(false).map { OpenAiCompletionChat(it, client) } +
        OpenAiModelIndex.completionModels(false).map { OpenAiCompletion(it, client) }

    override fun chatModels() =
        OpenAiModelIndex.chatModelsInclusive(false).map { OpenAiChat(it, client) }

    override fun visionLanguageModels() =
        OpenAiModelIndex.visionLanguageModels().map { OpenAiVisionLanguageChat(it, client) }

    override fun imageGeneratorModels() =
        OpenAiModelIndex.imageGeneratorModels().map { OpenAiImageGenerator(it, client) }

    override fun close() {
        client.client.close()
    }

}

