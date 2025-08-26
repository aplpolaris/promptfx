package tri.ai.tool

/**
 * Result of a tool executable execution.
 */
data class ToolExecutableResult(
    val result: String,
    val isTerminal: Boolean = false,
    val finalResult: String? = null
)