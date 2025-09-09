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

/** Strategy for decomposing tasks within a workflow. */
interface WorkflowTaskDecomposer {
    /** Decomposes pending tasks in the workflow. */
    fun decompose(state: WorkflowState): WorkflowTaskPlan
}

/** Results of a workflow planning step, consisting of a list of decomposed tasks. */
class WorkflowTaskPlan(
    /** New task decompositions resulting from the planning step. */
    val decomp: List<WorkflowTaskTree>
) {
    /** Returns a string representation of the task plan. */
    override fun toString() = decomp.joinToString("\n") { it.toString() }
}
