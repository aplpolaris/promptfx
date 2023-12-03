package tri.ai.prompt

/**
 * Configuration for running a series of prompt completions.
 * Values may be lists or constants.
 */
class AiPromptRunSeriesConfig {
    var model: Any = ""
    var modelParams: Map<String, Any> = mapOf()
    var prompt: Any = ""
    var promptParams: Map<String, Any> = mapOf()
    var runs = 1

    /** Get all run configs within this series. */
    fun runConfigs() = (1..runs).map { config(it - 1) }

    /** Get the i'th run config within this series. */
    fun config(i: Int) = AiPromptRunConfig().also {
        it.model = model.configIndex(i) as String
        it.modelParams = modelParams.entries.map {
            it.key to it.value.configIndex(i)
        }.toMap()
        it.prompt = prompt.configIndex(i) as String
        it.promptParams = promptParams.entries.map {
            it.key to it.value.configIndex(i)
        }.toMap()
    }

    private fun Any.configIndex(i: Int): Any = when (this) {
        is List<*> -> this[i % size]!!
        is Collection<*> -> throw UnsupportedOperationException()
        is Map<*, *> -> throw UnsupportedOperationException()
        else -> this
    }
}