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

import com.fasterxml.jackson.databind.node.TextNode
import tri.ai.core.agent.MAPPER
import tri.ai.core.tool.ExecContext
import tri.util.ANSI_GRAY
import tri.util.ANSI_GREEN
import tri.util.ANSI_RESET

/** Tracks state of workflow execution. */
class WorkflowState(_request: WorkflowUserRequest) {
    /** The user request that needs to be solved. */
    var request = _request
    /** Tree of tasks required for workflow execution. */
    val taskTree = WorkflowTaskTree.createSolveAndValidateTree(request)
    /** Context containing intermediate results and other useful information. */
    val context = ExecContext()
    /** History of solver steps taken. */
    val solveHistory = mutableListOf<WorkflowSolveStep>()
    /** Flag indicating when the execution is complete. */
    var isDone: Boolean = false
        private set

    init {
        // TODO - this is very brittle
        val userInput = request.request.substringAfter("\n").trim().substringAfter("\"\"\"").substringBefore("\"\"\"").trim()
        if (userInput.isNotEmpty())
            context.put("user_input", TextNode.valueOf(userInput))
    }

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

    /** Add task results to the context. */
    fun addResults(task: WorkflowTask, outputs: List<WVar>) {
        outputs.forEach { 
            val key = "${task.id}.${it.name}"
            val value = MAPPER.valueToTree<com.fasterxml.jackson.databind.JsonNode>(it.value)
            context.put(key, value)
        }
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

    /** Using the current plan, look up all the inputs associated with the given tool. */
    fun aggregateInputsFor(toolName: String) =
        ((taskTree.findTask { it is WorkflowTaskTool && it.tool == toolName }?.root as? WorkflowTaskTool)?.inputs
            ?: listOf()).associateWith { context.vars["$it.result"] ?: context.vars[it] }

    /** Get the final computed result from the context. */
    fun finalResult() =
        // TODO - this is brittle since it assumes a specific output exists on the context, want a general purpose solution
        context.vars[FINAL_RESULT_ID]!!.let { 
            if (it.isTextual) it.asText() else it.toString()
        }
}

