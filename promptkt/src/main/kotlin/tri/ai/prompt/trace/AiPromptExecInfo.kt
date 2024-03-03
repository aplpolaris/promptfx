package tri.ai.prompt.trace

import com.fasterxml.jackson.annotation.JsonInclude

/** Text inference execution info. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class AiPromptExecInfo(
    var error: String? = null,
    var queryTokens: Int? = null,
    var responseTokens: Int? = null,
    var responseTimeMillis: Long? = null
) {
    companion object {
        fun error(errorMessage: String?) = AiPromptExecInfo(error = errorMessage)
    }
}