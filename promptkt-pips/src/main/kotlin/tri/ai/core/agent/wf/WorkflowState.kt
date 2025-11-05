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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import tri.ai.core.tool.ExecContext
import tri.util.ANSI_GRAY
import tri.util.ANSI_GREEN
import tri.util.ANSI_RESET
import tri.util.MAPPER
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.ifEmpty

/** Tracks state of workflow execution. */
class WorkflowState(_request: WorkflowUserRequest) {

    /** The user request that needs to be solved. */
    var request = _request
    /** Tree of tasks required for workflow execution. */
    val taskTree = WorkflowTaskTree.createSolveAndValidateTree(request)
    /** Scratchpad containing intermediate results and other useful information. */
    val scratchpad = ExecContext()
    /** History of solver steps taken. */
    val solveHistory = mutableListOf<WorkflowSolveStep>()
    /** Flag indicating when the execution is complete. */
    var isDone: Boolean = false
        private set

    init {
        // TODO - this is very brittle
        val userInput = request.request.substringAfter("\n").trim().substringAfter("\"\"\"").substringBefore("\"\"\"").trim()
        if (userInput.isNotEmpty())
            scratchpad.put("user_input", MAPPER.valueToTree<JsonNode>(userInput))
        // description ?? scratchpad["user_input"] = WVar("user_input", "User input for the workflow", userInput)
    }

    /** Add task results to the scratchpad. */
    fun addResults(task: WorkflowTask, outputs: ObjectNode) {
        outputs.properties().forEach { (key, value) ->
            scratchpad.put("${task.id}.$key", value)
        }
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
    fun aggregateInputsFor(toolName: String): Map<String, JsonNode> =
        ((taskTree.findTask { it is WorkflowTaskTool && it.tool == toolName }?.root as? WorkflowTaskTool)?.inputs
            ?: listOf()).associateWith { scratchpad.vars["$it.$RESULT"] ?: scratchpad.vars[it]!! }

    /** Aggregates inputs as a string value. */
    fun aggregateInputsAsStringFor(toolName: String, taskName: String, separator: String = "\n"): String =
        aggregateInputsFor(toolName).values.mapNotNull { it.prettyPrint() }.ifEmpty { listOf(taskName) }.joinToString(separator)

    private fun JsonNode.prettyPrint() = when {
        isTextual -> asText()
        else -> toString()
    }

    /** Get the final computed result from the scratchpad. */
    // TODO - this is brittle since it assumes a specific output exists on the scratchpad, want a general purpose solution
    fun finalResult() =
        scratchpad.vars[FINAL_RESULT_ID]!!

}

