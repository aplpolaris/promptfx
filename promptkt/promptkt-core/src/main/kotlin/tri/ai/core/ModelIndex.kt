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

import com.fasterxml.jackson.module.kotlin.readValue
import tri.util.info
import tri.util.json.yamlMapper
import tri.util.loadResourceFromSiblingResources
import java.io.File
import java.io.InputStream

/** A model index with configurable model information and runtime overrides. */
open class ModelIndex(val modelFileName: String) {

    //region [ModelLibrary] LOADERS

    /** Model definitions in library. */
    private val models: ModelLibrary by lazy {
        val resource: InputStream? = loadResourceFromSiblingResources(this::class.java, modelFileName)
        if (resource == null) {
            info<ModelIndex>(
                "Model resource index not found: $modelFileName for ${this::class.java}. " +
                        "Using runtime configuration only."
            )
            ModelLibrary()
        } else {
            resource.use { yamlMapper.readValue(it) }
        }
    }
    /** Model overrides at runtime - ids only. */
    private val runtimeModels: ModelLibrary = setOf(File(modelFileName), File("config/$modelFileName"))
        .firstOrNull { it.exists() }?.let {
            yamlMapper.readValue(it)
        } ?: ModelLibrary()

    //endregion

    /** [ModelInfo] by id, where config in runtime overrides preconfigured info. */
    val modelInfoIndex by lazy { models.modelInfoIndex() + runtimeModels.modelInfoIndex() }
    /** All available model ids, including runtime overrides. */
    val modelIds by lazy { models.modelIds() + runtimeModels.modelIds() }

    /** Get audio models. */
    fun audioModels() = models(ModelLibrary::audio)
    /** Get chat models without vision-language models. */
    fun chatModels() = models(ModelLibrary::chat)
    /** Get text completion models. */
    fun completionModels() = models(ModelLibrary::completion)
    /** Get embedding models. */
    fun embeddingModels() = models(ModelLibrary::embeddings)
    /** Get image generator models. */
    fun imageGeneratorModels() = models(ModelLibrary::image_generator)
    /** Get moderation models. */
    fun moderationModels() = models(ModelLibrary::moderation)
    /** Get multimodal models. */
    fun multimodalModels() = models(ModelLibrary::multimodal)
    /** Get Responses API models. */
    fun responsesModels() = models(ModelLibrary::responses)
    /** Get text-to-speech models. */
    fun ttsModels() = models(ModelLibrary::tts)
    /** Get vision-language models. */
    @Deprecated("Use multimodalModels() instead")
    fun visionLanguageModels() = models(ModelLibrary::vision_language)

    //region HELPERS

    /** Get list of available models. */
    private fun models(op: ModelLibrary.() -> List<String>) =
        lookupModels(op(models), op(runtimeModels))
            .map { it.id }

    /**
     * Get list of models from two lists.
     * If no runtime models are provided, returns the preconfigured list, otherwise the runtime list.
     * If using the preconfigured list, check to make sure there are model definitions in the yaml (optional for runtime).
     */
    private fun lookupModels(preconfigured: List<String>, runtime: List<String>) =
        (runtime.ifEmpty { preconfigured }).map {
            modelInfoIndex[it] ?: ModelInfo(it, ModelType.UNKNOWN, "runtime")
        }

    //endregion

}
