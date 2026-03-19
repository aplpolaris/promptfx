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
    /**
     * General-purpose statistics map for execution metrics (token counts, timing, attempt count, etc.).
     * Use the string constants in the companion object as keys to avoid magic strings.
     */
    val stats: Map<String, Any> = mapOf()
) {
    /** Return true if the execution succeeded. */
    fun succeeded() = error == null && throwable == null

    // region DEPRECATED STAT PROPERTIES (backed by stats map)

    /** Number of query tokens. */
    @Deprecated("Use stats[QUERY_TOKENS]", ReplaceWith("stats[AiExecInfo.QUERY_TOKENS] as? Int"))
    val queryTokens: Int? get() = stats[QUERY_TOKENS] as? Int

    /** Number of response tokens. */
    @Deprecated("Use stats[RESPONSE_TOKENS]", ReplaceWith("stats[AiExecInfo.RESPONSE_TOKENS] as? Int"))
    val responseTokens: Int? get() = stats[RESPONSE_TOKENS] as? Int

    /** Response time in milliseconds. */
    @Deprecated("Use stats[RESPONSE_TIME_MILLIS]", ReplaceWith("stats[AiExecInfo.RESPONSE_TIME_MILLIS] as? Long"))
    val responseTimeMillis: Long? get() = stats[RESPONSE_TIME_MILLIS] as? Long

    /** Response time in milliseconds, total duration including retries. */
    @Deprecated("Use stats[RESPONSE_TIME_MILLIS_TOTAL]", ReplaceWith("stats[AiExecInfo.RESPONSE_TIME_MILLIS_TOTAL] as? Long"))
    val responseTimeMillisTotal: Long? get() = stats[RESPONSE_TIME_MILLIS_TOTAL] as? Long

    /** Number of executions attempted. */
    @Deprecated("Use stats[ATTEMPTS]", ReplaceWith("stats[AiExecInfo.ATTEMPTS] as? Int"))
    val attempts: Int? get() = stats[ATTEMPTS] as? Int

    /** Flag indicating whether this is an intermediate result. */
    @Deprecated("Use stats[INTERMEDIATE_RESULT]", ReplaceWith("stats[AiExecInfo.INTERMEDIATE_RESULT] as? Boolean"))
    val intermediateResult: Boolean? get() = stats[INTERMEDIATE_RESULT] as? Boolean

    // endregion

    companion object {
        /** Stats key for the number of query/prompt tokens. */
        const val QUERY_TOKENS = "queryTokens"
        /** Stats key for the number of response/completion tokens. */
        const val RESPONSE_TOKENS = "responseTokens"
        /** Stats key for the response time in milliseconds (last attempt). */
        const val RESPONSE_TIME_MILLIS = "responseTimeMillis"
        /** Stats key for the total response time in milliseconds, including all retries. */
        const val RESPONSE_TIME_MILLIS_TOTAL = "responseTimeMillisTotal"
        /** Stats key for the number of execution attempts. */
        const val ATTEMPTS = "attempts"
        /** Stats key for a flag indicating whether this is an intermediate result. */
        const val INTERMEDIATE_RESULT = "intermediateResult"

        /** Execution from a given time in millis. */
        fun durationSince(millis: Long, queryTokens: Int? = null, responseTokens: Int? = null) = AiExecInfo(
            stats = buildMap {
                put(RESPONSE_TIME_MILLIS, System.currentTimeMillis() - millis)
                queryTokens?.let { put(QUERY_TOKENS, it) }
                responseTokens?.let { put(RESPONSE_TOKENS, it) }
            }
        )

        /** Create an execution info with an error. */
        fun error(errorMessage: String?, throwable: Throwable? = null) = AiExecInfo(
            error = errorMessage,
            throwable = throwable
        )
    }
}
