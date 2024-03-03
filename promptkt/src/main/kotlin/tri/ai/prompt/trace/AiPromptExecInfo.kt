package tri.ai.prompt.trace

/** Text inference execution info. */
class AiPromptExecInfo(
    var error: String? = null,
    var queryTokens: Int? = null,
    var responseTokens: Int? = null,
    var responseTimeMillis: Long? = null
) {
    companion object {
        fun error(errorMessage: String?) = AiPromptExecInfo(error = errorMessage)
    }
}