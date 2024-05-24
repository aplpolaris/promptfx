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
package tri.ai.openai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.core.ModelInfo
import tri.ai.core.ModelType
import java.io.File

object OpenAiModels {

    //region MODEL ID'S

    private const val WHISPER_ID = "whisper-1"
    const val ADA_ID = "text-embedding-ada-002"
    private const val DALLE2_ID = "dall-e-2"
    private const val DALLE3_ID = "dall-e-3"
    private const val GPT35_TURBO_ID = "gpt-3.5-turbo"
    private const val GPT35_TURBO_INSTRUCT_ID = "gpt-3.5-turbo-instruct"

    //endregion

    private val MAPPER = ObjectMapper(YAMLFactory()).apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
    }

    /** List of model ids can be overridden at runtime. */
    private val RUNTIME_MODELS: OpenAiModelLibrary = File("openai-models.yaml").let {
        if (it.exists()) MAPPER.readValue(it) else OpenAiModelLibrary()
    }
    private val MODELS: OpenAiModelLibrary = MAPPER.readValue(OpenAiModels::class.java.getResourceAsStream("resources/openai-models.yaml")!!)
    private val FLAT_MODELS = MODELS.models.values.flatten()
    internal val MODEL_INDEX = FLAT_MODELS.associateBy { it.id } +
            FLAT_MODELS.flatMap { it.createSnapshots() }.associateBy { it.id }

    val AUDIO_WHISPER = MODEL_INDEX[WHISPER_ID]!!.id
    val EMBEDDING_ADA = MODEL_INDEX[ADA_ID]!!.id
    val IMAGE_DALLE2 = MODEL_INDEX[DALLE2_ID]!!.id
    val GPT35_TURBO = MODEL_INDEX[GPT35_TURBO_ID]!!.id
    val GPT35_TURBO_INSTRUCT = MODEL_INDEX[GPT35_TURBO_INSTRUCT_ID]!!.id

    fun chatModels(includeSnapshots: Boolean = false) = lookupModels(MODELS.chat, RUNTIME_MODELS.chat).flatMap {
        it.ids(includeSnapshots)
    }

    fun completionModels(includeSnapshots: Boolean = false) = lookupModels(MODELS.completion, RUNTIME_MODELS.completion).flatMap {
        it.ids(includeSnapshots)
    }

    fun embeddingModels() = lookupModels(MODELS.embeddings, RUNTIME_MODELS.embeddings).map { it.id }
    fun audioModels() = lookupModels(MODELS.audio, RUNTIME_MODELS.audio).map { it.id }
    fun ttsModels() = lookupModels(MODELS.tts, RUNTIME_MODELS.tts).map { it.id }
    fun imageGeneratorModels() = lookupModels(MODELS.image_generator, RUNTIME_MODELS.image_generator).map { it.id }
    fun visionLanguageModels() = lookupModels(MODELS.vision_language, RUNTIME_MODELS.vision_language).map { it.id }


    /**
     * Get list of models from two lists.
     * If no runtime models are provided, returns the preconfigured list, otherwise the runtime list.
     * If using the preconfigured list, check to make sure there are model definitions in the yaml (optional for runtime).
     *
     */
    private fun lookupModels(preconfigured: List<String>, runtime: List<String>) =
        if (runtime.isEmpty())
            preconfigured.map { MODEL_INDEX[it]!! }
        else
            runtime.map { ModelInfo(it, ModelType.UNKNOWN, "runtime") }

}

/** Configuration file for OpenAI API. */
class OpenAiModelLibrary {
    var models = mapOf<String, List<ModelInfo>>()
    var audio = listOf<String>()
    var chat = listOf<String>()
    var completion = listOf<String>()
    var embeddings = listOf<String>()
    var image_generator = listOf<String>()
    var moderation = listOf<String>()
    var tts = listOf<String>()
    var vision_language = listOf<String>()
}