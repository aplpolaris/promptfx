package tri.ai.prompt.trace

import com.fasterxml.jackson.annotation.JsonInclude

/** Text prompt generation info. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class AiPromptInfo(
    var prompt: String,
    var promptParams: Map<String, Any>
)