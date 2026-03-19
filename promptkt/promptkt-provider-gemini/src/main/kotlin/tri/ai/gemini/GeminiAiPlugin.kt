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
package tri.ai.gemini

import kotlinx.coroutines.runBlocking
import tri.ai.core.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Plugin registering models and services via the Gemini API. */
class GeminiAiPlugin : TextPlugin {

    val client = GeminiClient()

    override fun isApiConfigured() = client.settings.isConfigured()

    override fun modelSource() = GeminiModelIndex.MODEL_SOURCE

    override fun modelInfo() = if (client.settings.isConfigured())
        runBlocking {
            try {
                client.listModels().models.map { it.toCoreModelInfo() }
            } catch (x: Exception) {
                x.printStackTrace()
                emptyList()
            }
        }
    else emptyList()

    override fun embeddingModels() = models(GeminiModelIndex.embeddingModels()) { GeminiEmbeddingModel(it, client) }

    override fun chatModels() =
        models(GeminiModelIndex.chatModels()) { GeminiTextChat(it, client) }

    override fun multimodalModels() =
        models(GeminiModelIndex.multimodalModels()) { GeminiMultimodalChat(it, client) }

    override fun textCompletionModels() =
        models(GeminiModelIndex.completionModels() + GeminiModelIndex.chatModels()) {
            GeminiTextCompletion(it, client)
        }

    override fun imageGeneratorModels() = models(GeminiModelIndex.imageGeneratorModels()) { GeminiImageGenerator(it, modelSource(), client) }

    override fun speechToTextModels() =
        models(GeminiModelIndex.audioModels()) { GeminiSpeechToText(it, client) }

    override fun close() {
        client.close()
    }

    private fun <T> models(ids: List<String>, factory: (String) -> T): List<T> =
        if (!client.settings.isConfigured()) listOf() else ids.map(factory)

    //region GETTING MODEL INFO (WITH HEURISTICS)

    private fun ModelInfo.toCoreModelInfo() =
        ModelInfo(
            id = name.substringAfter("models/"),
            type = guessModelType(name.lowercase()),
            source = modelSource()
        ).also {
            it.metadata.name = displayName
            it.metadata.version = version
            it.metadata.description = description
            it.metadata.created = findReleaseDate(description)
            it.metadata.deprecation = findDeprecation(description)
            it.metadata.lifecycle = findLifecycle(it.id, description)

            it.capabilities.inputs = when (it.type) {
                ModelType.QUESTION_ANSWER -> listOf(DataModality.text)
                ModelType.TEXT_EMBEDDING -> listOf(DataModality.text)
                ModelType.TEXT_CHAT -> listOf(DataModality.text)
                ModelType.TEXT_VISION_CHAT -> listOf(DataModality.text, DataModality.image, DataModality.audio, DataModality.video)
                else -> null
            }
            it.capabilities.outputs = when (it.type) {
                ModelType.QUESTION_ANSWER -> listOf(DataModality.text)
                ModelType.TEXT_EMBEDDING -> listOf(DataModality.embedding)
                ModelType.TEXT_CHAT -> listOf(DataModality.text)
                ModelType.TEXT_VISION_CHAT -> listOf(DataModality.text)
                else -> null
            }

            it.params(
                "inputTokenLimit" to inputTokenLimit,
                "outputTokenLimit" to outputTokenLimit,
                "supportedGenerationMethods" to supportedGenerationMethods,
                "baseModelId" to baseModelId,
                "temperature" to temperature,
                "maxTemperature" to maxTemperature,
                "topP" to topP,
                "topK" to topK
            )
        }

    private fun guessModelType(id: String) = when {
        "embedding" in id -> ModelType.TEXT_EMBEDDING
        "image" in id -> ModelType.IMAGE_GENERATOR
        "nano-banana" in id -> ModelType.IMAGE_GENERATOR
        "tts" in id -> ModelType.TEXT_TO_SPEECH
        "audio" in id -> ModelType.SPEECH_TO_TEXT
        "gemini" in id -> ModelType.TEXT_CHAT
        "gemma" in id -> ModelType.TEXT_CHAT
        "aqa" in id -> ModelType.QUESTION_ANSWER
        "veo" in id -> ModelType.VIDEO_GENERATOR
        "sora" in id -> ModelType.VIDEO_GENERATOR
        else -> ModelType.UNKNOWN
    }

    private fun findDeprecation(description: String?): String? {
        return when {
            description == null -> null
            description.contains("will be discontinued on") ->
                description.substringAfter("will be discontinued on")
                    .substringBefore(".").parseDate()
            description.contains("was deprecated on") ->
                description.substringAfter("was deprecated on")
                    .substringBefore(".").parseDate()
            else -> null
        }?.toString()
    }

    private fun findReleaseDate(description: String?): LocalDate? {
        return when {
            description == null -> null
            description.contains("released in") ->
                description.substringAfter("released in")
                    .substringBefore(".").parseDate()
            description.contains("Experimental release (") ->
                description.substringAfter("Experimental release (")
                    .substringBefore(")").parseDate()
            else -> null
        }
    }

    private fun String?.parseDate(): LocalDate? {
        if (this == null) return null
        try {
            // common pattern e.g. "May of 2024"
            return LocalDate.parse(trim().replace(" of ", " 1, "), DateTimeFormatter.ofPattern("MMMM d, yyyy"))
        } catch (e: DateTimeParseException) {
            // ignore and try next
        }
        try {
            // common pattern e.g. "May 1st, 2025"
            val cleaned = replaceFirst(Regex("(\\d+)(st|nd|rd|th)"), "$1").trim()
            return LocalDate.parse(cleaned, DateTimeFormatter.ofPattern("MMMM d, yyyy"))
        } catch (e: DateTimeParseException) {
            // ignore and try next
        }
        return null
    }

    private fun findLifecycle(id: String, description: String?): ModelLifecycle {
        return when {
            "-exp" in id -> ModelLifecycle.EXPERIMENTAL
            description == null -> ModelLifecycle.PRODUCTION
            "Experimental release" in description -> ModelLifecycle.EXPERIMENTAL
            "released in" in description -> ModelLifecycle.PRODUCTION
            "most recent production" in description -> ModelLifecycle.PRODUCTION_ALIAS
            "legacy" in description -> ModelLifecycle.LEGACY
            "will be discontinued" in description -> ModelLifecycle.DEPRECATION_PLANNED
            "was deprecated" in description -> ModelLifecycle.DEPRECATED
            else -> ModelLifecycle.PRODUCTION
        }
    }

    //endregion

}
