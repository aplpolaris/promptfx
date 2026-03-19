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

import tri.ai.prompt.trace.AiTaskTrace

/** Result of a workflow execution. */
class AiWorkflowResult(val finalResult: AiTaskTrace, val interimResults: Map<String, AiTaskTrace>) {

    companion object {
        /** Return a result object indicating an error was thrown during execution. */
        fun error(message: String?, error: Throwable?) : AiWorkflowResult {
            val trace = AiTaskTrace.error(null, message, error)
            return AiWorkflowResult(trace, mapOf("result" to trace))
        }

        /** Return a result object indicating the workflow has not been implemented. */
        fun todo() = "This workflow is not yet implemented.".let {
            error(it, UnsupportedOperationException(it))
        }
    }

}

/** Wraps this as a workflow result. */
fun AiTaskTrace.asWorkflowResult() = AiWorkflowResult(this, mapOf("result" to this))
