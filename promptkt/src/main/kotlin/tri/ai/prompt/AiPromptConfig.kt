package tri.ai.prompt

import com.fasterxml.jackson.annotation.JsonInclude

/** Configuration for running a single prompt completion. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class AiPromptConfig(
    var model: String? = null,
    var modelParams: Map<String, Any> = mapOf(),
    var prompt: String = "",
    var promptParams: Map<String, Any> = mapOf()
)
