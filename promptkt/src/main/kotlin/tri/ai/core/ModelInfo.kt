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
package tri.ai.core

import java.time.LocalDate

/** Information about a model. */
class ModelInfo(var id: String, var type: ModelType, var source: String) {
    var name: String? = null
    var description: String? = null

    var created: LocalDate? = null
    var version: String? = null
    var deprecation: String? = null
    var lifecycle: ModelLifecycle = ModelLifecycle.UNKNOWN
    var snapshots: List<String> = listOf()

    var inputs: List<DataModality>? = null
    var outputs: List<DataModality>? = null
    var inputTokenLimit: Int? = null
    var outputTokenLimit: Int? = null
    var totalTokenLimit: Int? = null

    var outputDimension: Int? = null

    var params: MutableMap<String, Any> = mutableMapOf()

    override fun toString() =
        "$id ($type $source)"

    /** Sets non-null parameters based on provided list of key-value pairs. */
    fun params(vararg pairs: Pair<String, Any?>) {
        pairs.forEach {
            if (it.second != null)
                params[it.first] = it.second!!
        }
    }

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

/** Data modality for inputs and outputs. */
enum class DataModality {
    text, audio, image, video, embedding, moderation
}

/** Model types. */
enum class ModelType {
    TEXT_COMPLETION,
    TEXT_CHAT,
    TEXT_VISION_CHAT,
    TEXT_EMBEDDING,
    IMAGE_GENERATOR,
    TEXT_TO_SPEECH,
    SPEECH_TO_TEXT,
    AUDIO_CHAT,
    REALTIME_CHAT,
    MODERATION,
    QUESTION_ANSWER,
    UNKNOWN
}

/** Stages of model lifecycle. */
enum class ModelLifecycle {
    /** Model is in development. */
    EXPERIMENTAL,
    /** Model is in production. */
    PRODUCTION,
    /** Model is an alias for a production model. */
    PRODUCTION_ALIAS,
    /** Legacy model. */
    LEGACY,
    /** Model is planned for deprecation. */
    DEPRECATION_PLANNED,
    /** Model is deprecated. */
    DEPRECATED,
    /** Model has been discontinued. */
    DISCONTINUED,
    /** Lifecycle is unknown. */
    UNKNOWN
}