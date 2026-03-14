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

import tri.util.ANSI_GRAY
import tri.util.ANSI_GREEN
import tri.util.ANSI_RESET

/** Tracks the plan and history of a workflow execution, focused on managing the task plan. */
class WorkflowPlanState(_request: WorkflowUserRequest) {

    /** The user request that needs to be solved. */
    var request = _request
    /** Tree of tasks required for workflow execution. */
    val taskTree = WorkflowTaskTree.createSolveAndValidateTree(request)
    /** History of solver steps taken. */
    val solveHistory = mutableListOf<WorkflowSolveStep>()
    /** Flag indicating when the execution is complete. */
    var isDone: Boolean = false
        private set

    /** Check for completion, updating the isDone flag if all tasks are complete. */
    fun checkDone() {
        isDone = taskTree.isDone
    }

    /** Updates task tree based on a planning result. */
    fun updateTasking(plan: WorkflowTaskPlan) {
        plan.decomp.forEach { newNode ->
            val curNode = taskTree.findTask { it == newNode.root } ?: error("Task not found: ${newNode.root}")
            curNode.tasks.clear()
            curNode.tasks.addAll(newNode.tasks)
            curNode.isDone = newNode.isDone
        }
        printTaskPlan(plan.decomp)
    }

    internal fun printTaskPlan(trees: List<WorkflowTaskTree>, indent: String = ""): String = StringBuilder().apply {
        trees.forEach { tree ->
            append(ANSI_GRAY)
            append(indent)
            if (tree.root.isDone)
                append("${ANSI_GREEN}✔${ANSI_RESET} ")
            else
                append("❓")
            if ((tree.root as? WorkflowTaskTool)?.id != null)
                append("[${tree.root.id}] ")
            append(tree.root.name)
            if (!tree.root.description.isNullOrBlank())
                append(": ${tree.root.description}")
            if ((tree.root as? WorkflowTaskTool)?.inputs.let { it != null && !it.isEmpty() })
                append(", Inputs: ${(tree.root as WorkflowTaskTool).inputs}")
            append(ANSI_RESET)
            appendLine()
            if (tree.tasks.isNotEmpty()) {
                append(printTaskPlan(tree.tasks, indent = indent.removeSuffix("- ") + "  - "))
                appendLine()
            }
        }
    }.toString().trim()

}
