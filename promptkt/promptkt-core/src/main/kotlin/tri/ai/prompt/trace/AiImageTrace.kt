/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.prompt.trace

/**
 * Splits this trace into individual traces, one per image output.
 * Each returned trace contains a single output from the original list.
 * Returns an empty list if this trace has no output.
 */
fun AiTaskTrace.splitImages(): List<AiTaskTrace> =
    output?.outputs?.map { copy(outputInfo = AiOutputInfo(listOf(it))) } ?: emptyList()

/**
 * Details of an executed image prompt, including prompt configuration, model configuration, execution metadata, and output.
 * Not designed for serialization (yet).
 *
 * @deprecated Use [AiTaskTrace] directly. The [splitImages] extension function on [AiTaskTrace] replaces
 * [splitImages]. Construct an [AiTaskTrace] with the same parameters via its primary or backward-compat constructor.
 */
@Deprecated(
    message = "Use AiTaskTrace directly. splitImages() is available as an extension function on AiTaskTrace.",
    replaceWith = ReplaceWith("AiTaskTrace", "tri.ai.prompt.trace.AiTaskTrace")
)
class AiImageTrace(
    promptInfo: PromptInfo?,
    modelInfo: AiModelInfo?,
    execInfo: AiExecInfo = AiExecInfo(),
    outputInfo: AiOutputInfo? = AiOutputInfo(listOf())
) : AiTaskTrace(promptInfo, modelInfo, execInfo, outputInfo) {

    override fun toString() = "AiImageTrace(taskId='$taskId', promptInfo=$prompt, modelInfo=$model, execInfo=$exec, outputInfo=$output)"

    /** Splits this image trace into individual images. */
    @Suppress("DEPRECATION")
    fun splitImages(): List<AiImageTrace> =
        output!!.outputs.map {
            AiImageTrace(prompt, model, exec, AiOutputInfo(listOf(it)))
        }

    override fun copy(
        promptInfo: PromptInfo?,
        modelInfo: AiModelInfo?,
        execInfo: AiExecInfo,
        outputInfo: AiOutputInfo?,
        callerId: String?,
        parentTaskId: String?,
        viewId: String?
    ) = AiImageTrace(promptInfo, modelInfo, execInfo, outputInfo).also {
        it.taskId = taskId
        it.parentTaskId = parentTaskId
        it.callerId = viewId ?: callerId
    }

}
