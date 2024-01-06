/*-
 * #%L
 * promptkt-0.1.0-SNAPSHOT
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

/** Result of executing a task. */
data class AiTaskResult<T>(val value: T? = null, val error: Throwable? = null, val modelId: String?) {

    fun <S> map(function: (T) -> S) = AiTaskResult(value?.let { function(value) }, error, modelId)

    /** Wraps this as a pipeline result. */
    fun asPipelineResult() = AiPipelineResult("result", mapOf("result" to this))

    companion object {
        /** Task with token result. */
        fun <T> result(value: T, modelId: String? = null) =
            AiTaskResult(value, null, modelId)

        /** Task not attempted because input was invalid. */
        fun invalidRequest(message: String) =
            AiTaskResult(message, IllegalArgumentException(message), null)

        /** Task not attempted or successful because of a general error. */
        fun error(message: String, error: Throwable) =
            AiTaskResult(message, error, null)
    }

}
