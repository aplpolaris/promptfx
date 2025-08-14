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
package tri.ai.tool.wf

/** Advances workflow towards a solution. */
abstract class WorkflowSolver(
    val name: String,
    val description: String,
    val inputs: List<WParam>,
    val outputs: List<WParam>
) {
    constructor(name: String, description: String, inputs: Map<String, String>, outputs: Map<String, String>):
            this(name, description, inputs.entries.map { WParam(it.key, it.value) }, outputs.entries.map { WParam(it.key, it.value) })

    /** Perform a solve step on the given execution state object. */
    abstract suspend fun solve(state: WorkflowState, task: WorkflowTask): WorkflowSolveStep

    fun toPlaintext() = "$name: $description"

    /** Compile results of a solve task into a [WorkflowSolveStep]. */
    fun solveStep(task: WorkflowTask, inputs: List<WVar>, outputs: List<WVar>, executionTimeMillis: Long, isSuccess: Boolean) =
        WorkflowSolveStep(task, this, inputs, outputs, executionTimeMillis, isSuccess)

    fun inputs(vararg value: Any) = inputs.mapIndexed { i, param ->
        WVar(param.name, param.description, value[i])
    }

    fun outputs(vararg value: Any) = outputs.mapIndexed { i, param ->
        WVar(param.name, param.description, value[i])
    }

    fun input(i: Int, value: Any) = WVar(inputs[i].name, inputs[i].description, value)
    fun output(i: Int, value: Any) = WVar(outputs[i].name, outputs[i].description, value)

}

/** Information about a variable. */
class WParam(
    val name: String,
    val description: String
) {
    fun withValue(value: Any) = WVar(name, description, value)
}

/** Logs a step taken during workflow execution. */
data class WorkflowSolveStep(
    val task: WorkflowTask,
    val solver: WorkflowSolver,
    val inputs: List<WVar>,
    val outputs: List<WVar>,
    val executionTimeMillis: Long,
    val isSuccess: Boolean
)

/** An intermediate value during workflow execution. */
data class WVar(
    val name: String,
    val description: String,
    val value: Any
)
