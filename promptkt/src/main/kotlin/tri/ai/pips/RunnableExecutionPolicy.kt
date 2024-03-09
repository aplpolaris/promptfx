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

import tri.util.info
import java.time.Duration

/** Policy for re-attempting failed executions of a given runnable. */
class RunnableExecutionPolicy(
    /** Max number of times to reattempt a failed execution. */
    var maxRetries: Int = 3,
    /** Initial delay before first retry. */
    var initialRetryDelay: Long = 1000L,
    /** Factor by which to increase the delay between retries. */
    var retryBackoff: Double = 1.5,
) {

    /** Executes a runnable with given policy. */
    suspend fun <T> execute(runnable: suspend () -> T): AiTaskResult<T> {
        var retries = 0
        var delay = initialRetryDelay
        val t00 = System.currentTimeMillis()
        while (true) {
            val t0 = System.currentTimeMillis()
            try {
                val result = runnable()
                val t1 = System.currentTimeMillis()
                return AiTaskResult(value = result,
                    duration = Duration.ofMillis(t1 - t0),
                    durationTotal = Duration.ofMillis(t1 - t00),
                    attempts = retries + 1
                )
            } catch (x: Exception) {
                val t1 = System.currentTimeMillis()
                if (retries++ >= maxRetries)
                    return AiTaskResult(null, error = x,
                        duration = Duration.ofMillis(t1 - t0),
                        durationTotal = Duration.ofMillis(t1 - t00),
                        attempts = retries
                    )
                info<RunnableExecutionPolicy>("Failed with ${x.message}. Retrying after ${Duration.ofMillis(t0 - t00)}...")
                kotlinx.coroutines.delay(delay)
                delay = (delay * retryBackoff).toLong()
            }
        }
    }

}