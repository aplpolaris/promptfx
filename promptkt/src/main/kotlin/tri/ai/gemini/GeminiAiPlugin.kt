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
package tri.ai.gemini

import kotlinx.coroutines.runBlocking
import tri.ai.core.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Plugin registering models and services via the Gemini API. */
class GeminiAiPlugin : TextPlugin {

    private val client = GeminiClient()

    override fun modelSource() = "Gemini"

    override fun modelInfo() = if (client.isConfigured())
        runBlocking {
            try {
                client.listModels().models.map { it.toCoreModelInfo() }
            } catch (x: Exception) {
                x.printStackTrace()
                emptyList()
            }
        }
    else listOf()

    override fun embeddingModels() = models(GeminiModelIndex.embeddingModels()) { GeminiEmbeddingService(it, client) }

    override fun chatModels() =
        models(GeminiModelIndex.chatModelsInclusive()) { GeminiTextChat(it, client) }

    override fun multimodalModels() =
        models(GeminiModelIndex.multimodalModels()) { GeminiMultimodalChat(it, client) }

    override fun textCompletionModels() =
        models(GeminiModelIndex.completionModels() + GeminiModelIndex.chatModelsInclusive()) {
            GeminiTextCompletion(it, client)
        }

    override fun visionLanguageModels() =
        models(GeminiModelIndex.visionLanguageModels()) { GeminiVisionLanguageChat(it, client) }

    override fun imageGeneratorModels() = models(GeminiModelIndex.imageGeneratorModels()) { GeminiImageGenerator(it, client) }

    override fun close() {
        client.close()
    }

    private fun <T> models(ids: List<String>, factory: (String) -> T): List<T> =
        if (!client.isConfigured()) listOf() else ids.map(factory)

    //region GETTING MODEL INFO (WITH HEURISTICS)

    private fun ModelInfo.toCoreModelInfo() =
        ModelInfo(
            id = name.substringAfter("models/"),
            type = ModelType.UNKNOWN,
            source = modelSource()
        ).also {
            it.name = displayName
            it.version = version
            it.description = description
            it.inputTokenLimit = inputTokenLimit
            it.outputTokenLimit = outputTokenLimit

            it.created = findReleaseDate(description)
            it.deprecation = findDeprecation(description)
            it.lifecycle = findLifecycle(it.id, description)
            it.type = findType(it.id, supportedGenerationMethods.toSet())
            it.inputs = when (it.type) {
                ModelType.QUESTION_ANSWER -> listOf(DataModality.text)
                ModelType.TEXT_EMBEDDING -> listOf(DataModality.text)
                ModelType.TEXT_CHAT -> listOf(DataModality.text)
                ModelType.TEXT_VISION_CHAT -> listOf(DataModality.text, DataModality.image, DataModality.audio, DataModality.video)
                else -> null
            }
            it.outputs = when (it.type) {
                ModelType.QUESTION_ANSWER -> listOf(DataModality.text)
                ModelType.TEXT_EMBEDDING -> listOf(DataModality.embedding)
                ModelType.TEXT_CHAT -> listOf(DataModality.text)
                ModelType.TEXT_VISION_CHAT -> listOf(DataModality.text)
                else -> null
            }

            it.params(
                "supportedGenerationMethods" to supportedGenerationMethods,
                "baseModelId" to baseModelId,
                "temperature" to temperature,
                "maxTemperature" to maxTemperature,
                "topP" to topP,
                "topK" to topK
            )
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

    /**
     * This is mostly done through trial and error.
     * See https://ai.google.dev/gemini-api/docs/models/gemini#model-variations for inputs supported.
     */
    private fun findType(id: String, methods: Set<String>): ModelType {
        val ANSWER = "generateAnswer"
        val BIDI_GENERATE = "bidiGenerateContent"
        val CACHED = "createCachedContent"
        val COUNT = "countTokens"
        val EMBED = "embedContent"
        val GENERATE = "generateContent"
        val TUNED = "createTunedModel"

        val GENERATE2 = "generateMessage"
        val COUNT2 = "countMessageTokens"

        val COUNT3 = "countTextTokens"
        val EMBED3 = "embedText"
        val GENERATE3 = "generateText"
        val TUNED3 = "createTunedTextModel"

        var type = when (methods) {
            setOf(EMBED) -> ModelType.TEXT_EMBEDDING
            setOf(EMBED3, COUNT3) -> ModelType.TEXT_EMBEDDING
            setOf(GENERATE) -> ModelType.TEXT_CHAT
            setOf(GENERATE, COUNT) -> ModelType.TEXT_CHAT
            setOf(GENERATE, COUNT, TUNED) -> ModelType.TEXT_CHAT
            setOf(GENERATE2, COUNT2) -> ModelType.TEXT_CHAT
            setOf(GENERATE, COUNT2, TUNED) -> ModelType.TEXT_CHAT
            setOf(GENERATE3, COUNT3, TUNED3) -> ModelType.TEXT_CHAT
            setOf(ANSWER) -> ModelType.QUESTION_ANSWER
            setOf(GENERATE, COUNT, CACHED) -> ModelType.TEXT_CHAT
            setOf(GENERATE, COUNT, BIDI_GENERATE) -> ModelType.TEXT_CHAT
            else -> ModelType.UNKNOWN
        }

        if (type == ModelType.TEXT_CHAT && ("vision" in id || "gemini-1.5" in id || "gemini-2.0" in id)) {
            type = ModelType.TEXT_VISION_CHAT
        }
        return type
    }

    //endregion

}
