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
package tri.ai.core.agent.wf

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.Executable
import tri.util.json.tryJson

/** Advances workflow towards a solution. */
abstract class WorkflowSolver(
    override val name: String,
    override val description: String,
    override val version: String,
    override val inputSchema: JsonNode,
    override val outputSchema: JsonNode
): Executable {
    constructor(name: String, description: String, version: String, inputSchema: String, outputSchema: String):
            this(name, description, version, inputSchema.tryJson()!!, outputSchema.tryJson()!!)

    /** Provide a plaintext description of this solver for use in prompts. */
    fun descriptionForPrompts() = "$name: $description"

    /** Perform a solve step on the given execution state object. */
    abstract suspend fun solve(state: WorkflowState, task: WorkflowTask): WorkflowSolveStep

    override suspend fun execute(input: JsonNode, context: ExecContext): JsonNode {
        TODO("Not implemented and TBD whether this is used - considering deprecating solve and the broader [WorkflowState] in favor of the [ExecContext]")
    }
}

/** Logs a step taken during workflow execution. */
data class WorkflowSolveStep(
    val task: WorkflowTask,
    val solver: WorkflowSolver,
    val inputs: ObjectNode,
    val outputs: ObjectNode,
    val executionTimeMillis: Long,
    val isSuccess: Boolean
)
