package tri.promptfx.ui

import tri.ai.prompt.trace.*

/** Result including the trace and formatted text. */
class FormattedPromptTraceResult(val trace: AiPromptTrace<String>, val formattedOutputs: List<FormattedText>)
    : AiPromptTraceSupport<String>(trace.promptInfo, trace.modelInfo, trace.execInfo, trace.outputInfo) {

    override fun toString() = trace.outputInfo?.outputs?.joinToString() ?: "null"

    override fun copy(promptInfo: AiPromptInfo?, modelInfo: AiPromptModelInfo?, execInfo: AiPromptExecInfo) =
        FormattedPromptTraceResult(AiPromptTrace(promptInfo, modelInfo, execInfo, outputInfo), formattedOutputs)

}