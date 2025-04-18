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
package tri.ai.prompt.trace.batch

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.core.TextCompletion
import tri.ai.openai.jsonMapper
import tri.ai.openai.yamlMapper
import tri.ai.prompt.trace.AiPromptInfo
import tri.ai.prompt.trace.AiModelInfo

/**
 * Provides a series of prompt/model pairings for execution.
 * The series is based on a list of configs for prompts and a list of configs for models.
 * Supports cycling through either lists, or lists provided within model/prompt parameters.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class AiPromptBatchCyclic(id: String) : AiPromptBatch(id) {

    var model: Any = ""
    var modelParams: Map<String, Any> = mapOf()
    var prompt: Any = ""
    var promptParams: Map<String, Any> = mapOf()
    var runs = 1

    /** Get all run configs within this series. */
    override fun runConfigs(modelLookup: (String) -> TextCompletion) = (1..runs).map { config(it - 1, modelLookup) }

    /** Get the i'th run config within this series. */
    private fun config(i: Int, modelLookup: (String) -> TextCompletion): AiPromptRunConfig {
        return AiPromptRunConfig(
            AiPromptInfo(
                prompt.configIndex(i) as String,
                promptParams.entries.associate { it.key to it.value.configIndex(i) }
            ), AiModelInfo(
                model.configIndex(i) as String,
                modelParams.entries.associate { it.key to it.value.configIndex(i) }
            ), modelLookup
        )
    }

    private fun Any.configIndex(i: Int): Any = when (this) {
        is List<*> -> this[i % size]!!
        is Collection<*> -> throw UnsupportedOperationException()
        is Map<*, *> -> throw UnsupportedOperationException()
        else -> this
    }

    companion object {
        /** Get a batch to repeat the same prompt/model pairings for a number of runs. */
        fun repeat(batchId: String, prompt: AiPromptInfo, model: AiModelInfo, runs: Int) = AiPromptBatchCyclic(batchId).apply {
            this.prompt = prompt.prompt
            this.promptParams = prompt.promptParams
            this.model = model.modelId
            this.modelParams = model.modelParams
            this.runs = runs
        }

        fun fromJson(json: String) = jsonMapper.readValue<AiPromptBatchCyclic>(json)
        fun fromYaml(yaml: String) = yamlMapper.readValue<AiPromptBatchCyclic>(yaml)
    }

}
