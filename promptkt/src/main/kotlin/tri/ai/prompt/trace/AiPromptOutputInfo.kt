package tri.ai.prompt.trace

import com.fasterxml.jackson.annotation.JsonInclude

/** Text inference output info. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class AiPromptOutputInfo(
    var output: String? = null
)