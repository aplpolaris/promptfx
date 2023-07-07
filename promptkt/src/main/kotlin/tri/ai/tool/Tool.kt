package tri.ai.tool

/** General purpose functionality that can be leveraged by an LLM agent or prompt. */
abstract class Tool(
    val name: String,
    val description: String,
    val notes: String? = null,
    val requiresLlm: Boolean = false,
    val isTerminal: Boolean = false
) {
    abstract suspend fun run(input: String): String
}

/** The result of running a tool. */
class ToolResult(val historyText: String, val finalResult: String? = null)
