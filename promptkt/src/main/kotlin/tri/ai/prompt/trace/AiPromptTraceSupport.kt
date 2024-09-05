/*-
 * #%L
 * tri.promptfx:promptkt
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

import com.fasterxml.jackson.annotation.JsonIgnore
import tri.ai.pips.AiPipelineResult
import java.util.UUID.randomUUID

/** Common elements of a prompt trace. */
abstract class AiPromptTraceSupport<T>(
    var promptInfo: AiPromptInfo?,
    var modelInfo: AiPromptModelInfo?,
    var execInfo: AiPromptExecInfo,
    var outputInfo: AiPromptOutputInfo<T>? = null
) {

    /** Unique identifier for this trace. */
    var uuid = randomUUID().toString()

    /** Make a copy of this trace with updated information. */
    abstract fun copy(
        promptInfo: AiPromptInfo? = this.promptInfo,
        modelInfo: AiPromptModelInfo? = this.modelInfo,
        execInfo: AiPromptExecInfo = this.execInfo
    ): AiPromptTraceSupport<T>

    /** Get all outputs, if present. */
    @get:JsonIgnore
    val values: List<T>?
        get() = outputInfo?.outputs

    /** Get the first output value, if it exists, otherwise throw [NoSuchElementException]. */
    @get:JsonIgnore
    val firstValue: T
        get() = outputInfo?.outputs?.firstOrNull() ?: throw NoSuchElementException("No output value")

    /** Get error message, if present. */
    @get:JsonIgnore
    val errorMessage: String?
        get() = execInfo.error ?: execInfo.throwable?.message

    /**
     * Wraps this as a pipeline result.
     * If [promptInfo] and [modelInfo] are provided, result will also be wrapped in [AiPromptTrace].
     */
    fun asPipelineResult() = AiPipelineResult(this, mapOf("result" to this))

}