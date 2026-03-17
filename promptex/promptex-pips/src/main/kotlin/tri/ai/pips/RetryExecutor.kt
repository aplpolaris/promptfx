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
package tri.ai.pips

import tri.ai.core.tool.ExecContext
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiPromptTraceSupport
import tri.util.info
import java.time.Duration

/**
 * Policy for re-attempting failed executions of a given runnable.
 * Tracks number of attempts and total duration within [AiPromptTrace].
 */
class RetryExecutor(
    /** Max number of times to reattempt a failed execution. */
    maxRetries: Int = 3,
    /** Initial delay before first retry. */
    initialRetryDelay: Long = 1000L,
    /** Factor by which to increase the delay between retries. */
    retryBackoff: Double = 1.5,
) {
    val retry = SimpleRetryExecutor(maxRetries, initialRetryDelay, retryBackoff)

    /**
     * Executes a task with given policy. Logs trace information to [ExecContext.traces] with additional
     * metadata about the number of attempts and total duration.
     */
    suspend fun execute(task: AiTask<*, *>, input: Any?, context: ExecContext): Any? {
        return retry.execute({
            @Suppress("UNCHECKED_CAST")
            (task as AiTask<Any?, Any?>).execute(input, context)
        }, onSuccess = { it ->
            // output may be null when a task logs an error trace and returns null (rather than throwing)
            val output = it.value
            // Update trace in context if one was logged, enriching it with retry metadata
            val existingTrace = context.trace(task.id)
            if (existingTrace != null) {
                context.logTrace(task.id, existingTrace.copy(
                    execInfo = existingTrace.exec.copy(
                        responseTimeMillis = it.attemptTime,
                        responseTimeMillisTotal = it.totalTime,
                        attempts = it.attempts
                    )
                ))
            } else {
                check(output !is AiPromptTraceSupport) {
                    "Task '${task.id}' returned AiPromptTraceSupport directly. Use context.logTrace() instead of returning traces from execute()."
                }
            }
            output
        }, onFailure = {
            val errorTrace = AiPromptTrace.error(
                modelInfo = null,
                message = it.error!!.message,
                throwable = it.error,
                duration = it.attemptTime,
                durationTotal = it.totalTime,
                attempts = it.attempts
            )
            context.logTrace(task.id, errorTrace)
            errorTrace
        })
    }

}

/** Executes a runnable multiple times with a retry policy. */
class SimpleRetryExecutor(
    var maxRetries: Int = 3,
    var initialRetryDelay: Long = 1000L,
    var retryBackoff: Double = 1.5
) {
    suspend fun <S, T> execute(task: suspend () -> S, onSuccess: (RetryTaskResult<S>) -> T, onFailure: (RetryTaskResult<S>) -> T): T {
        var retries = 0
        var delay = initialRetryDelay
        val t00 = System.currentTimeMillis()
        while (true) {
            val t0 = System.currentTimeMillis()
            try {
                val success = task()
                return onSuccess(RetryTaskResult(success, retries + 1, t00, t0))
            } catch (e: Exception) {
                if (retries++ >= maxRetries)
                    return onFailure(RetryTaskResult(null, retries, t00, t0, error = e))
                info<RetryExecutor>("Failed with ${e.message}. Retrying after ${Duration.ofMillis(t0 - t00)}...")
                kotlinx.coroutines.delay(delay)
                delay = (delay * retryBackoff).toLong()
            }
        }
    }
}

/** Representation of a retry task result. */
class RetryTaskResult<T>(
    val value: T?,
    val attempts: Int,
    val overallStartTime: Long,
    val attemptStartTime: Long,
    val attemptEndTime: Long = System.currentTimeMillis(),
    val error: Exception? = null
) {
    val totalTime: Long
        get() = attemptEndTime - overallStartTime
    val attemptTime: Long
        get() = attemptEndTime - attemptStartTime
}
