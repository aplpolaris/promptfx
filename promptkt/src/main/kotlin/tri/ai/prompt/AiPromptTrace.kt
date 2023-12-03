package tri.ai.prompt

/** Information about a prompt and associated results. */
class AiPromptTrace(
    var runConfig: AiPromptRunConfig,
    var output: String? = null
) {
    var error: String? = null
    var queryTokens: Int? = null
    var responseTokens: Int? = null
    var responseTimeMillis: Long? = null

    override fun toString() = runConfig.toString() + "\n" +
        "Output: $output\n" +
        mapOf("error" to error, "queryTokens" to queryTokens, "responseTokens" to responseTokens, "responseTimeMillis" to responseTimeMillis).filterValues { it != null }.toString()

    companion object {
        fun error(config: AiPromptRunConfig, errorMessage: String?) = AiPromptTrace(config).apply {
            error = errorMessage
        }
    }
}