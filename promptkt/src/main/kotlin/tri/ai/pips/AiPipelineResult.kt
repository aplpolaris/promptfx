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

/** Result of a pipeline execution. */
class AiPipelineResult(finalTaskId: String, val results: Map<String, AiTaskResult<*>>) {

    /** The result of the last task in the pipeline. */
    val finalResult: Any? = results[finalTaskId]?.value?.let {
        when (it) {
            is AiPipelineResult -> it.finalResult
            is AiTaskResult<*> -> it.value
            else -> it
        }
    }

    companion object {
        fun error(message: String, error: Throwable) = AiPipelineResult(
            "error",
            mapOf("error" to AiTaskResult.error(message, error))
        )

        fun todo() = error("This pipeline is not yet implemented.", UnsupportedOperationException())
    }

}
