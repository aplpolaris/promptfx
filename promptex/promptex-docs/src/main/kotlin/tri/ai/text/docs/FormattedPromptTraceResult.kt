/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package tri.ai.text.docs

import com.fasterxml.jackson.annotation.JsonIgnore
import tri.ai.prompt.trace.*

/**
 * Creates a copy of this trace annotated with a list of [FormattedText] outputs.
 * This is the preferred replacement for constructing a [FormattedPromptTraceResult].
 */
@Suppress("DEPRECATION")
fun AiTaskTrace.withFormattedOutputs(outputs: List<FormattedText>): FormattedPromptTraceResult =
    FormattedPromptTraceResult(this, outputs)

/**
 * Returns the formatted text outputs if this trace carries them, or `null` otherwise.
 * This is the preferred replacement for `(trace as? FormattedPromptTraceResult)?.formattedOutputs`.
 */
@Suppress("DEPRECATION")
val AiTaskTrace.formattedOutputs: List<FormattedText>?
    get() = (this as? FormattedPromptTraceResult)?.formattedOutputs

/**
 * Result including the trace and formatted text.
 *
 * @deprecated Use [AiTaskTrace] directly. Pair a trace with its formatted outputs using
 * [withFormattedOutputs] and retrieve them via the [formattedOutputs] extension property.
 */
@Deprecated(
    message = "Use AiTaskTrace directly. Use withFormattedOutputs() extension to attach formatted outputs, and formattedOutputs extension property to retrieve them.",
    replaceWith = ReplaceWith("AiTaskTrace", "tri.ai.prompt.trace.AiTaskTrace")
)
class FormattedPromptTraceResult(trace: AiTaskTrace, @get:JsonIgnore val formattedOutputs: List<FormattedText>)
    : AiTaskTrace(trace.prompt, trace.model, trace.exec, trace.output) {

    init {
        // The backward-compat constructor called by the super() call generates a new random taskId.
        // Restore the identity fields from the source trace so they are preserved.
        taskId = trace.taskId
        parentTaskId = trace.parentTaskId
        callerId = trace.callerId
    }

    override fun toString() = output?.outputs?.joinToString() ?: "null"

    override fun copy(
        promptInfo: PromptInfo?,
        modelInfo: AiModelInfo?,
        execInfo: AiExecInfo,
        outputInfo: AiOutputInfo?,
        callerId: String?,
        parentTaskId: String?,
        viewId: String?
    ) = FormattedPromptTraceResult(
        AiTaskTrace(promptInfo, modelInfo, execInfo, outputInfo).also {
            it.taskId = taskId
            it.parentTaskId = parentTaskId
            it.callerId = viewId ?: callerId
        },
        formattedOutputs
    )

}
