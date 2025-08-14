/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
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
import tri.ai.prompt.trace.AiPromptTraceSupport

/** Result of a pipeline execution. */
class AiPipelineResult<T>(val finalResult: AiPromptTraceSupport<T>, val interimResults: Map<String, AiPromptTraceSupport<*>>) {

    companion object {
        /** Return a result object indicating an error was thrown during execution. */
        fun <T> error(message: String?, error: Throwable?) : AiPipelineResult<T> {
            val trace = AiPromptTrace.error<T>(null, message, error)
            return AiPipelineResult(trace, mapOf("result" to trace))
        }

        /** Return a result object indicating the pipeline has not been implemented. */
        fun <T> todo() = "This pipeline is not yet implemented.".let {
            error<T>(it, UnsupportedOperationException(it))
        }
    }

}

/**
 * Wraps this as a pipeline result.
 * If [prompt] and [model] are provided, result will also be wrapped in [AiPromptTrace].
 */
fun <T> AiPromptTraceSupport<T>.asPipelineResult() = AiPipelineResult(this, mapOf("result" to this))

