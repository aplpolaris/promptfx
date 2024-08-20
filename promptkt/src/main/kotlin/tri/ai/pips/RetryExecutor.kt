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

import tri.ai.prompt.trace.AiPromptTrace
import tri.util.info
import java.time.Duration

/**
 * Policy for re-attempting failed executions of a given runnable.
 * Tracks number of attempts and total duration within [AiTaskResult], and [AiPromptTrace] if the result is a prompt trace object.
 */
class RetryExecutor(
    /** Max number of times to reattempt a failed execution. */
    var maxRetries: Int = 3,
    /** Initial delay before first retry. */
    var initialRetryDelay: Long = 1000L,
    /** Factor by which to increase the delay between retries. */
    var retryBackoff: Double = 1.5,
) {

    /**
     * Executes a task with given policy. Adds additional information about the execution to [AiTaskResult]
     * related to the number of attempts and total duration.
     */
    suspend fun <T> execute(task: AiTask<T>, inputs: Map<String, AiTaskResult<*>>, monitor: AiTaskMonitor): AiTaskResult<T> {
        var retries = 0
        var delay = initialRetryDelay
        val t00 = System.currentTimeMillis()
        while (true) {
            val t0 = System.currentTimeMillis()
            try {
                val success = task.execute(inputs, monitor)
                val t1 = System.currentTimeMillis()
                return success.copy(
                    duration = Duration.ofMillis(t1 - t0),
                    durationTotal = Duration.ofMillis(t1 - t00),
                    attempts = retries + 1
                ).also {
                    // TODO - this hard-coded type check is brittle, potentially unexpected side effect behavior
                    (it.values!![0] as? AiPromptTrace)?.execInfo?.let { ei ->
                        ei.error = it.errorMessage
                        ei.responseTimeMillis = it.duration?.toMillis()
                    }
                }
            } catch (x: Exception) {
                val t1 = System.currentTimeMillis()
                if (retries++ >= maxRetries)
                    return AiTaskResult(
                        error = x,
                        duration = Duration.ofMillis(t1 - t0),
                        durationTotal = Duration.ofMillis(t1 - t00),
                        attempts = retries
                    )
                info<RetryExecutor>("Failed with ${x.message}. Retrying after ${Duration.ofMillis(t0 - t00)}...")
                kotlinx.coroutines.delay(delay)
                delay = (delay * retryBackoff).toLong()
            }
        }
    }

}