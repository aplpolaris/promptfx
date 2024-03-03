package tri.ai.prompt.run

import com.fasterxml.jackson.annotation.JsonInclude
import tri.ai.prompt.trace.AiPromptInfo
import tri.ai.prompt.trace.AiPromptModelInfo

/**
 * Provides a series of prompt/model pairings for execution.
 * The series is based on a list of configs for prompts and a list of configs for models.
 * Supports cycling through either lists, or lists provided within model/prompt parameters.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class AiPromptBatchCyclic : AiPromptBatch {

    var model: Any = ""
    var modelParams: Map<String, Any> = mapOf()
    var prompt: Any = ""
    var promptParams: Map<String, Any> = mapOf()
    var runs = 1

    /** Get all run configs within this series. */
    override fun runConfigs() = (1..runs).map { config(it - 1) }

    /** Get the i'th run config within this series. */
    private fun config(i: Int): Pair<AiPromptInfo, AiPromptModelInfo> {
        return AiPromptInfo(
            prompt.configIndex(i) as String,
            promptParams.entries.associate { it.key to it.value.configIndex(i) }
        ) to AiPromptModelInfo(
            model.configIndex(i) as String,
            modelParams.entries.associate { it.key to it.value.configIndex(i) }
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
        fun repeat(prompt: AiPromptInfo, model: AiPromptModelInfo, runs: Int) = AiPromptBatchCyclic().apply {
            this.prompt = prompt.prompt
            this.promptParams = prompt.promptParams
            this.model = model.model
            this.modelParams = model.modelParams
            this.runs = runs
        }
    }

}