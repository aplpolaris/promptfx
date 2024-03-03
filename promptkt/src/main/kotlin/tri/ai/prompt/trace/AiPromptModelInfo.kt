package tri.ai.prompt.trace

import com.fasterxml.jackson.annotation.JsonInclude

/** Model configuration info. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class AiPromptModelInfo(
    var model: String,
    var modelParams: Map<String, Any> = mapOf()
)