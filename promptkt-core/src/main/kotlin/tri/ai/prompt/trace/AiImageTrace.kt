/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
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
 * Details of an executed image prompt, including prompt configuration, model configuration, execution metadata, and output.
 * Not designed for serialization (yet).
 */
class AiImageTrace(
    promptInfo: PromptInfo?,
    modelInfo: AiModelInfo?,
    execInfo: AiExecInfo = AiExecInfo(),
    outputInfo: AiOutputInfo? = AiOutputInfo(listOf())
) : AiPromptTraceSupport(promptInfo, modelInfo, execInfo, outputInfo) {

    override fun toString() = "AiImageTrace(uuid='$uuid', promptInfo=$prompt, modelInfo=$model, execInfo=$exec, outputInfo=$output)"

    /** Splits this image trace into individual images. */
    fun splitImages(): List<AiImageTrace> =
        output!!.outputs.map {
            AiImageTrace(prompt, model, exec, AiOutputInfo(listOf(it)))
        }

    override fun copy(promptInfo: PromptInfo?, modelInfo: AiModelInfo?, execInfo: AiExecInfo) =
        AiImageTrace(promptInfo, modelInfo, execInfo, output)

}
