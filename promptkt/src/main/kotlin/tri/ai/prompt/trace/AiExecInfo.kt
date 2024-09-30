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

/** Text inference execution info. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class AiExecInfo(
    /** Error message, if any. */
    var error: String? = null,
    /** Throwable error, if any. */
    @JsonIgnore
    var throwable: Throwable? = null,
    /** Number of query tokens. */
    var queryTokens: Int? = null,
    /** Number of response tokens. */
    var responseTokens: Int? = null,
    /** Response time in milliseconds. */
    var responseTimeMillis: Long? = null,
    /** Response time in milliseconds, total duration including retries. */
    var responseTimeMillisTotal: Long? = null,
    /** Number of executions attempted. */
    val attempts: Int? = null,
    /** Flag indicating whether this is an intermediate result. */
    val intermediateResult: Boolean? = null,
    /** Id of view that initiated the execution. */
    val viewId: String? = null
) {
    /** Return true if the execution succeeded. */
    fun succeeded() = error == null && throwable == null

    companion object {
        /** Execution from a given time in millis. */
        fun durationSince(millis: Long, queryTokens: Int? = null, responseTokens: Int? = null) = AiExecInfo(
            responseTimeMillis = System.currentTimeMillis() - millis,
            queryTokens = queryTokens,
            responseTokens = responseTokens
        )
        /** Create an execution info with an error. */
        fun error(errorMessage: String?, throwable: Throwable? = null) = AiExecInfo(
            error = errorMessage,
            throwable = throwable
        )
    }
}
