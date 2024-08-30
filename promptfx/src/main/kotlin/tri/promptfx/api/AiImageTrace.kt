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
package tri.promptfx.api

import tri.ai.prompt.trace.AiPromptExecInfo
import tri.ai.prompt.trace.AiPromptInfo
import tri.ai.prompt.trace.AiPromptModelInfo
import tri.ai.prompt.trace.AiPromptTraceSupport
import java.util.*

/**
 * Details of an executed image prompt, including prompt configuration, model configuration, execution metadata, and output.
 * Not designed for serialization (yet).
 */
class AiImageTrace(
    promptInfo: AiPromptInfo,
    modelInfo: AiPromptModelInfo,
    execInfo: AiPromptExecInfo = AiPromptExecInfo(),
    var outputInfo: AiImageOutputInfo = AiImageOutputInfo(listOf())
) : AiPromptTraceSupport(promptInfo, modelInfo, execInfo) {
    /** Unique identifier for this trace. */
    var uuid = UUID.randomUUID().toString()

    override fun toString() = "AiImageTrace(uuid='$uuid', promptInfo=$promptInfo, modelInfo=$modelInfo, execInfo=$execInfo, outputInfo=$outputInfo)"

    /** Splits this image trace into individual images. */
    fun splitImages(): List<AiImageTrace> =
        outputInfo.imageUrls.map {
            AiImageTrace(promptInfo, modelInfo, execInfo, AiImageOutputInfo(listOf(it)))
        }
}

/** Output info for an image prompt. */
class AiImageOutputInfo(
    var imageUrls: List<String>
)
