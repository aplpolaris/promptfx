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
}

/** A model index with configurable model information and runtime overrides. */
abstract class ModelIndex(val modelFileName: String) {

    private val MAPPER = ObjectMapper(YAMLFactory()).apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
    }

    /** List of model ids can be overridden at runtime. */
    private val RUNTIME_MODELS: ModelLibrary = File(modelFileName).let {
        if (it.exists()) MAPPER.readValue(it) else ModelLibrary()
    }
    private val MODELS: ModelLibrary = MAPPER.readValue(javaClass.getResourceAsStream("resources/$modelFileName")!!)
    private val FLAT_MODELS = MODELS.models.values.flatten()
    internal val MODEL_INDEX = FLAT_MODELS.associateBy { it.id } +
            FLAT_MODELS.flatMap { it.createSnapshots() }.associateBy { it.id }

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
            MODEL_INDEX[it] ?: ModelInfo(it, ModelType.UNKNOWN, "runtime")
        }

}