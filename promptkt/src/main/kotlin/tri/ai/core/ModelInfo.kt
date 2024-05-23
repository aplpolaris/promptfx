package tri.ai.core

import java.time.LocalDate

/** Information about a model. */
class ModelInfo(var id: String, var type: ModelType, var source: String) {
    var name: String? = null
    var description: String? = null

    var created: LocalDate? = null
    var version: String? = null
    var deprecation: String? = null
    var snapshots: List<String> = listOf()

    var inputTokenLimit: Int? = null
    var outputTokenLimit: Int? = null
    var totalTokenLimit: Int? = null

    var outputDimension: Int? = null

    var params: MutableMap<String, Any> = mutableMapOf()

    override fun toString() =
        "id ($type $source)"

    /** Get id's of models, including snapshots. */
    fun ids(includeSnapshots: Boolean) =
        if (includeSnapshots)
            listOf(id) + snapshots.map { "$id-$it" }
        else
            listOf(id)

    /** Generate list of snapshot models. */
    fun createSnapshots() = snapshots.map {
        ModelInfo("$id-$it", type, source).also {
            it.name = "$name ($it)"
            it.description = description
            it.version = version
            it.deprecation = deprecation
            it.inputTokenLimit = inputTokenLimit
            it.outputTokenLimit = outputTokenLimit
            it.totalTokenLimit = totalTokenLimit
            it.outputDimension = outputDimension
        }
    }
}

enum class ModelType {
    TEXT_COMPLETION,
    TEXT_CHAT,
    TEXT_VISION_CHAT,
    TEXT_EMBEDDING,
    VISION_LANGUAGE,
    IMAGE_GENERATOR,
    TEXT_TO_SPEECH,
    SPEECH_TO_TEXT,
    MODERATION,
    QUESTION_ANSWER,
    UNKNOWN
}