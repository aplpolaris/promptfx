/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
    promptInfo: AiPromptInfo?,
    modelInfo: AiPromptModelInfo?,
    execInfo: AiPromptExecInfo = AiPromptExecInfo(),
    outputInfo: AiPromptOutputInfo<String>? = AiPromptOutputInfo(listOf())
) : AiPromptTraceSupport<String>(promptInfo, modelInfo, execInfo, outputInfo) {

    override fun toString() = "AiImageTrace(uuid='$uuid', promptInfo=$promptInfo, modelInfo=$modelInfo, execInfo=$execInfo, outputInfo=$outputInfo)"

    /** Splits this image trace into individual images. */
    fun splitImages(): List<AiImageTrace> =
        outputInfo!!.outputs.map {
            AiImageTrace(promptInfo, modelInfo, execInfo, AiPromptOutputInfo(listOf(it)))
        }

    override fun copy(promptInfo: AiPromptInfo?, modelInfo: AiPromptModelInfo?, execInfo: AiPromptExecInfo) =
        AiImageTrace(promptInfo, modelInfo, execInfo, outputInfo)

}