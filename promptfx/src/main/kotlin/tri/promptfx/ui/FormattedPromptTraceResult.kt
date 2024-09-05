package tri.promptfx.ui

import tri.ai.prompt.trace.*

/** Result including the trace and formatted text. */
class FormattedPromptTraceResult(val trace: AiPromptTrace<String>, val formattedOutputs: List<FormattedText>)
    : AiPromptTraceSupport<String>(trace.prompt, trace.model, trace.exec, trace.output) {

    override fun toString() = trace.output?.outputs?.joinToString() ?: "null"

    override fun copy(promptInfo: AiPromptInfo?, modelInfo: AiModelInfo?, execInfo: AiExecInfo) =
        FormattedPromptTraceResult(AiPromptTrace(promptInfo, modelInfo, execInfo, output), formattedOutputs)

}