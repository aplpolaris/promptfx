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

/** Strategies for executing dynamic workflows. */
interface WorkflowExecutorStrategy {
    /**
     * Given the current state, including a task tree with tasks in various stages of completion,
     * determine an appropriate next step in the workflow, by specifically selecting a task and associated solver for that task.
     * @throws WorkflowToolNotFoundException
     * @throws WorkflowTaskNotFoundException
     */
    suspend fun nextSolver(state: WorkflowState, solvers: List<WorkflowSolver>): Pair<WorkflowSolver, WorkflowTask>
    /**
     * Identify a task checker to either break up a task, or determine that the initial workflow tasking has been solved.
     */
    suspend fun decomposeTask(state: WorkflowState, solvers: List<WorkflowSolver>): WorkflowTaskPlan
}
