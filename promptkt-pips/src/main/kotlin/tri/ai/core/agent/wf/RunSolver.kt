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
package tri.ai.core.agent.wf

/** Solver that takes a single input, provides a single output, based on a runner. */
class RunSolver(
    name: String,
    description: String,
    inputDescription: String? = null,
    outputDescription: String,
    val run: (String) -> String
) : WorkflowSolver(name, description, inputDescription?.let { mapOf(INPUT to it) } ?: mapOf(), mapOf(RESULT to outputDescription)) {
    override suspend fun solve(
        state: WorkflowState,
        task: WorkflowTask
    ): WorkflowSolveStep {
        val t0 = System.currentTimeMillis()
        val inputData = if (inputs.isEmpty()) "" else
            state.aggregateInputsFor(name).values.joinToString("\n") { 
                it?.let { node -> 
                    if (node.isTextual) node.asText() else node.toString()
                } ?: ""
            }
        val result = run(inputData)
        return solveStep(
            task,
            if (inputs.isEmpty()) inputs() else inputs(inputData),
            outputs(result),
            System.currentTimeMillis() - t0,
            true
        )
    }

}