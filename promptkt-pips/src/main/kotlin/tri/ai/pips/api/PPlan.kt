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
package tri.ai.pips.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import tri.util.MAPPER
import tri.util.YAML_MAPPER

/** A serializable version of a plan. */
data class PPlan(
    /** The optional ID of the plan. */
    val id: String? = null,
    /** The list of steps in the plan. */
    val steps: List<PPlanStep>
) {
    companion object {
        /** Construct plan from JSON. */
        fun parse(json: String) = MAPPER.readValue<PPlan>(json)
        /** Construct plan from YAML. */
        fun parseYaml(yaml: String) = YAML_MAPPER.readValue<PPlan>(yaml)
        /** An empty plan with no ID and no steps. */
        val EMPTY = PPlan(null, emptyList())
    }
}

/** A serializable version of a plan step. */
data class PPlanStep(
    /** Reference to the tool to be used. */
    val tool: String,
    /** Description of the execution step. */
    val description: String? = null,
    /** The input to the tool. */
    val input: JsonNode,
    /** Optional location to save tool result. */
    val saveAs: String? = null,
    /** Optional instruction for handling errors. */
    val onError: OnError = OnError.Fail,
    /** Optional timeout for the tool execution in milliseconds. */
    val timeoutMs: Long? = null,
)

/** Instruction for handling errors during tool execution. */
enum class OnError {
    /** Retry the step on error. */
    Retry,
    /** Continue to the next step on error. */
    Continue,
    /** Fail the entire plan execution on error. */
    Fail
}
