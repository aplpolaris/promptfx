package tri.ai.prompt.trace

import java.util.UUID.randomUUID

/** Common elements of a prompt trace. */
abstract class AiPromptTraceSupport(
    var promptInfo: AiPromptInfo?,
    var modelInfo: AiPromptModelInfo?,
    var execInfo: AiPromptExecInfo
) {

    /** Unique identifier for this trace. */
    var uuid = randomUUID().toString()

    /** Make a copy of this trace with updated information. */
    abstract fun copy(
        promptInfo: AiPromptInfo? = this.promptInfo,
        modelInfo: AiPromptModelInfo? = this.modelInfo,
        execInfo: AiPromptExecInfo = this.execInfo
    ): AiPromptTraceSupport

}