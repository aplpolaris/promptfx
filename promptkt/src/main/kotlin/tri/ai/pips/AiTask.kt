package tri.ai.pips

/** Task that can be executed by AI or API. */
abstract class AiTask<T>(
    val id: String,
    val description: String? = null,
    val dependencies: Set<String> = setOf()
) {
    abstract suspend fun execute(inputs: Map<String, AiTaskResult<*>>, monitor: AiTaskMonitor): AiTaskResult<T>
}

