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
import com.fasterxml.jackson.annotation.JsonInclude
import tri.ai.pips.AiPipelineResult

/** Details of an executed prompt, including prompt configuration, model configuration, execution metadata, and output. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class AiPromptTrace<T>(
    promptInfo: AiPromptInfo? = null,
    modelInfo: AiPromptModelInfo? = null,
    execInfo: AiPromptExecInfo = AiPromptExecInfo(),
    outputInfo: AiPromptOutputInfo<T>? = null
) : AiPromptTraceSupport<T>(promptInfo, modelInfo, execInfo, outputInfo) {

    override fun toString() =
        "AiPromptTrace(promptInfo=$promptInfo, modelInfo=$modelInfo, execInfo=$execInfo, outputInfo=$outputInfo)"

    override fun copy(
        promptInfo: AiPromptInfo?,
        modelInfo: AiPromptModelInfo?,
        execInfo: AiPromptExecInfo
    ): AiPromptTrace<T> = AiPromptTrace(promptInfo, modelInfo, execInfo, outputInfo)

    /** Convert output using a provided function, without modifying any other parts of the response. */
    fun <S> mapOutput(transform: (T) -> S) = AiPromptTrace<S>(
        promptInfo,
        modelInfo,
        execInfo,
        outputInfo?.map(transform)
    )

    /** Convert output using a provided function, without modifying any other parts of the response. */
    fun <S> mapOutputList(transform: (List<T>) -> S) = AiPromptTrace<S>(
        promptInfo,
        modelInfo,
        execInfo,
        outputInfo?.mapList(transform)
    )

    companion object {
        /** Create a trace with a successful result. */
        fun <T> result(res: T, modelId: String? = null, duration: Long? = null, durationTotal: Long? = duration) = AiPromptTrace<T>(
            modelInfo = modelId?.let { AiPromptModelInfo(it) },
            execInfo = AiPromptExecInfo(responseTimeMillis = duration, responseTimeMillisTotal = durationTotal),
            outputInfo = AiPromptOutputInfo.output(res)
        )

        /** Task with given results. */
        fun <T> results(values: List<T>, modelId: String? = null, duration: Long? = null, durationTotal: Long? = duration) = AiPromptTrace<T>(
            modelInfo = modelId?.let { AiPromptModelInfo(it) },
            execInfo = AiPromptExecInfo(responseTimeMillis = duration, responseTimeMillisTotal = durationTotal),
            outputInfo = AiPromptOutputInfo(values)
        )

        /** Create an execution info with an error. */
        fun <T> error(message: String?, throwable: Throwable? = null, responseTimeMillis: Long? = null, responseTimeMillisTotal: Long? = null, attempts: Int? = null) =
            AiPromptTrace<T>(null, null,
                AiPromptExecInfo(message, throwable, responseTimeMillis = responseTimeMillis, responseTimeMillisTotal = responseTimeMillisTotal, attempts = attempts)
            )

        /** Task not attempted because input was invalid. */
        fun <T> invalidRequest(message: String) =
            error<T>(message, IllegalArgumentException(message))
    }

}

