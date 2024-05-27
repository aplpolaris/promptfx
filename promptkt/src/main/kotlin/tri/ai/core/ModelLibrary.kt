/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

/** Library of models, including model information and list of enabled models. */
class ModelLibrary {
    var models = mapOf<String, List<ModelInfo>>()
    var audio = listOf<String>()
    var chat = listOf<String>()
    var completion = listOf<String>()
    var embeddings = listOf<String>()
    var image_generator = listOf<String>()
    var moderation = listOf<String>()
    var tts = listOf<String>()
    var vision_language = listOf<String>()

    /** Create model index with unique identifiers, including any registered snapshots. */
    fun modelInfoIndex() = models.values.flatten().flatMap { listOf(it) + it.createSnapshots() }.associateBy { it.id }
}

/** A model index with configurable model information and runtime overrides. */
abstract class ModelIndex(val modelFileName: String) {

    private val MAPPER = ObjectMapper(YAMLFactory()).apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
    }

    /** Model definitions in library. */
    private val MODELS: ModelLibrary = MAPPER.readValue(javaClass.getResourceAsStream("resources/$modelFileName")!!)
    /** Model overrides at runtime - ids only. */
    private val RUNTIME_MODELS: ModelLibrary = setOf(File(modelFileName), File("config/$modelFileName"))
        .firstOrNull { it.exists() }?.let {
            MAPPER.readValue(it)
        } ?: ModelLibrary()
    /** [ModelInfo] by id, where config in runtime overrides preconfigured info. */
    internal val MODEL_INFO_INDEX = MODELS.modelInfoIndex() + RUNTIME_MODELS.modelInfoIndex()

    fun chatModels(includeSnapshots: Boolean = false) = models(ModelLibrary::chat, includeSnapshots)
    fun completionModels(includeSnapshots: Boolean = false) = models(ModelLibrary::completion, includeSnapshots)
    fun embeddingModels(includeSnapshots: Boolean = false) = models(ModelLibrary::embeddings, includeSnapshots)
    fun moderationModels(includeSnapshots: Boolean = false) = models(ModelLibrary::moderation, includeSnapshots)
    fun audioModels(includeSnapshots: Boolean = false) = models(ModelLibrary::audio, includeSnapshots)
    fun ttsModels(includeSnapshots: Boolean = false) = models(ModelLibrary::tts, includeSnapshots)
    fun imageGeneratorModels(includeSnapshots: Boolean = false) = models(ModelLibrary::image_generator, includeSnapshots)
    fun visionLanguageModels(includeSnapshots: Boolean = false) = models(ModelLibrary::vision_language, includeSnapshots)

    /** Get list of available models. */
    private fun models(op: ModelLibrary.() -> List<String>, includeSnapshots: Boolean = false) =
        lookupModels(op(MODELS), op(RUNTIME_MODELS))
            .flatMap { it.ids(includeSnapshots) }

    /**
     * Get list of models from two lists.
     * If no runtime models are provided, returns the preconfigured list, otherwise the runtime list.
     * If using the preconfigured list, check to make sure there are model definitions in the yaml (optional for runtime).
     */
    private fun lookupModels(preconfigured: List<String>, runtime: List<String>) =
        (runtime.ifEmpty { preconfigured }).map {
            MODEL_INFO_INDEX[it] ?: ModelInfo(it, ModelType.UNKNOWN, "runtime")
        }

}
