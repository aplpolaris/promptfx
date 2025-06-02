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

import com.fasterxml.jackson.module.kotlin.readValue
import okio.IOException
import tri.ai.core.MAPPER
import tri.ai.core.TextCompletion
import tri.util.ANSI_GRAY
import tri.util.ANSI_RESET
import tri.util.warning

/**
 * Uses an LLM for workflow planning and tool (solver) selection.
 */
class WExecutorChat(val completionEngine: TextCompletion, val maxTokens: Int, val temp: Double) :  WorkflowExecutorStrategy {

    // use the LLM and the known set of solvers to select a sequence of tasks for a task in the workflow
    // for now to simplify things, we'll just limit this to the root "problem" in the workflow
    // we always have a final task to wrap things up, validating that the result matches the user's question
    override suspend fun decomposeTask(state: WorkflowState, solvers: List<WorkflowSolver>): WorkflowTaskPlan {
        if (state.taskTree.findTask { it == state.request }!!.tasks.isNotEmpty()) {
            println("${ANSI_GRAY}Problem task tree is not empty, skipping task decomposition.$ANSI_RESET")
            return WorkflowTaskPlan(listOf())
        }

        // build the prompt using current state
        val rootTask = state.request
        val toolsPlaintext = solvers.joinToString("\n") { " - ${it.toPlaintext()}" }
        val prompt = PROMPTS.fill(
            "planner",
            "problem" to rootTask.request,
            "tools_details" to toolsPlaintext
        )

        // use LLM to generate a response
        val response = completionEngine.complete(prompt, tokens = maxTokens, temperature = temp)

        // parse the response and use it to build a set of subtasks to solve
        val taskDecomp = parseTaskDecomp(response.firstValue)
        val taskTree = when {
            taskDecomp.subtasks.isEmpty() ->
                error("Empty task decomposition: ${taskDecomp.problem}")
            taskDecomp.subtasks.size == 1 && taskDecomp.subtasks.first().task == taskDecomp.problem ->
                WorkflowTaskTree(rootTask, listOf())
            else ->
                WorkflowTaskTree(rootTask, taskDecomp.subtasks.map {
                    WorkflowTaskTool(id = it.id, name = it.task, tool = it.tool, inputs = it.inputs)
                }.map {
                    WorkflowTaskTree(it)
                })
        }
        return WorkflowTaskPlan(listOf(taskTree))
    }

    // since the LLM has already selected a suggested tool at this point, we can just lookup and return that solver
    override suspend fun nextSolver(state: WorkflowState, solvers: List<WorkflowSolver>): Pair<WorkflowSolver, WorkflowTask> {
        val task = state.taskTree.findTask { it is WorkflowTaskTool && !it.isDone }?.root as? WorkflowTaskTool
            ?: state.taskTree.findTask { it is WorkflowValidatorTask }?.root
            ?: throw WorkflowTaskNotFoundException("Unable to find task in workflow state")
        val solver = when (task) {
            is WorkflowValidatorTask -> WValiditySolver(completionEngine, maxTokens, temp)
            is WorkflowTaskTool -> solvers.find { it.name == task.tool }
                ?: throw WorkflowToolNotFoundException("Unable to find solver for task: ${task.tool}")
            else -> error("Unexpected branch")
        }
        return solver to task
    }

    private fun parseTaskDecomp(response: String): TaskDecomp {
        val quotedResponse = response.findCode()
        // parse into yaml and return
        return try {
            MAPPER.readValue<TaskDecomp>(quotedResponse)
        } catch (e: IOException) {
            warning<WExecutorChat>("Failed to parse task decomposition from response:\n$quotedResponse", e)
            TaskDecomp("", emptyList())
        }
    }
}

fun String.findCode() = substringAfter("```").substringAfter("\n").substringBefore("```").trim()

/** Class for task decomposition prompt result. */
data class TaskDecomp(
    val problem: String,
    val subtasks: List<TaskDecompSubtask>
)

/** Class for a subtask with noted inputs. */
data class TaskDecompSubtask(
    val id: String,
    val task: String,
    val tool: String,
    val inputs: List<String>
)
