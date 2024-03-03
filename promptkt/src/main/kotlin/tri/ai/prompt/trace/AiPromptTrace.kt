package tri.ai.prompt.trace

import com.fasterxml.jackson.annotation.JsonInclude
import java.util.UUID.randomUUID

/** Details of an executed prompt, including prompt configuration, model configuration, execution metadata, and output. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class AiPromptTrace(
    var promptInfo: AiPromptInfo,
    var modelInfo: AiPromptModelInfo,
    var execInfo: AiPromptExecInfo,
    var outputInfo: AiPromptOutputInfo = AiPromptOutputInfo(null)
) {

    /** Unique identifier for this trace. */
    var uuid = randomUUID().toString()

    override fun toString() = listOf("Prompt: $promptInfo", "Model: $modelInfo", "Exec: $execInfo", "Output: $outputInfo").joinToString("\n")

}

