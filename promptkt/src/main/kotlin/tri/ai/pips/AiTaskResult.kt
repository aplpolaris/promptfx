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
package tri.ai.pips

import tri.ai.prompt.trace.*
import java.time.Duration

/**
 * Result of executing a task.
 * Includes optional parameters for errors, model ids, execution duration, retry attempts.
 */
data class AiTaskResult<T>(
    /** Result of the task. */
    val values: List<T>? = null,
    /** Error message, if any. */
    val errorMessage: String? = null,
    /** Error that occurred during execution, if any. */
    val error: Throwable? = null,
    /** Model ID of the task, if any. */
    val modelId: String? = null,
    /** Duration of first successful or final retry. */
    val duration: Duration? = null,
    /** Total duration including retries. */
    val durationTotal: Duration? = null,
    /** Number of executions attempted. */
    val attempts: Int? = null,
) {

    constructor(
        value: T,
        errorMessage: String? = null,
        error: Throwable? = null,
        modelId: String? = null,
        duration: Duration? = null,
        durationTotal: Duration? = null,
        attempts: Int? = null,
    ) : this(listOf(value), errorMessage, error, modelId, duration, durationTotal, attempts)

    /** First return result (or only one if there is a single response). */
    val firstValue
        get() = values?.firstOrNull()

    /** Applies an operation to the result value, if present. All other values are copied directly. */
    fun <S> mapvalue(function: (T) -> S) = AiTaskResult(
        values?.let { it.map { function(it) } },
        errorMessage, error, modelId, duration, durationTotal, attempts
    )

    /** Applies an operation to the list of result values, if present. All other values are copied directly. */
    fun <S> maplist(function: (List<T>?) -> S) = AiTaskResult(
        function(values),
        errorMessage, error, modelId, duration, durationTotal, attempts
    )

    /**
     * Wraps this as a pipeline result.
     * If [promptInfo] and [modelInfo] are provided, result will also be wrapped in [AiPromptTrace].
     */
    fun asPipelineResult(promptInfo: AiPromptInfo? = null, modelInfo: AiPromptModelInfo? = null): AiPipelineResult {
        if (promptInfo != null && modelInfo != null) {
            return mapvalue {
                AiPromptTrace(
                    promptInfo,
                    modelInfo,
                    AiPromptExecInfo(errorMessage, responseTimeMillis = durationTotal?.toMillis()),
                    AiPromptOutputInfo(values)
                )
            }.asPipelineResult()
        }
        return AiPipelineResult("result", mapOf("result" to this))
    }

    companion object {
        /** Task with given results. */
        fun <T> results(values: List<T>, modelId: String? = null) =
            AiTaskResult(values, modelId = modelId)

        /** Task with given result. */
        fun <T> result(value: T, modelId: String? = null) =
            AiTaskResult(value, modelId = modelId)

        /** Task not attempted because input was invalid. */
        fun <T> invalidRequest(message: String) =
            error<T>(message, IllegalArgumentException(message))

        /** Task not attempted or successful because of a general error. */
        fun <T> error(message: String, error: Throwable) =
            AiTaskResult<T>(errorMessage = message, error = error)
    }

}
