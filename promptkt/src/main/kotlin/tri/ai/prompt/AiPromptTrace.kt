package tri.ai.prompt

import com.fasterxml.jackson.annotation.JsonInclude

/** Information about a prompt and associated results. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class AiPromptTrace(
    var runConfig: AiPromptConfig,
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
        fun error(config: AiPromptConfig, errorMessage: String?) = AiPromptTrace(config).apply {
            error = errorMessage
        }
    }
}