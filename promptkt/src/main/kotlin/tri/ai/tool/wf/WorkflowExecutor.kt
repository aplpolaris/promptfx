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

import kotlinx.coroutines.runBlocking
import tri.util.ANSI_CYAN
import tri.util.ANSI_GRAY
import tri.util.ANSI_GREEN
import tri.util.ANSI_RED
import tri.util.ANSI_RESET

/**
 * Dynamic workflow executor, works by handing off execution to various solvers, which might perform task decomposition
 * steps and/or building up a solution. Solvers are also responsible for determining if the task is "done".
 */
class WorkflowExecutor(
    val strategy: WorkflowExecutorStrategy,
    val solvers: List<WorkflowSolver>
) {
    private var maxSteps = 8
    private val LOGGER = WorkflowLogger

    /** Solve the given problem using a dynamic workflow execution. */
    fun solve(problem: WorkflowUserRequest): WorkflowState {
        val state = WorkflowState(problem)

        runBlocking {
            var i = 1
            val t0 = System.currentTimeMillis()
            while (!state.isDone) {
                if (i > maxSteps) {
                    LOGGER.failedExecutionMajor("Too many steps, stopping execution.")
                    break
                }
                LOGGER.executionStep("Workflow Step ${i++}")

                // 1. Do a planning step, which may involve breaking up a task into smaller tasks
                val updatedTasks = strategy.decomposeTask(state, solvers)
                LOGGER.execution("Task Planning:")
                if (updatedTasks.decomp.isNotEmpty())
                    state.updateTasking(updatedTasks)

                // 2. Select a solver and task to work on
                val (solver, task) = strategy.nextSolver(state, solvers)
                LOGGER.executionProgress("Solver", solver.name)
                LOGGER.executionProgress("Task", task.name)

                // 3. Use the selected solver to solve part of the task
                val step = solver.solve(state, task)
                if (step.isSuccess) {
                    state.taskTree.setTaskDone(task)
                    state.scratchpad.addResults(task, step.outputs)
                    LOGGER.executionProgressLine("Solve Inputs","- ${step.inputs.joinToString("\n - ") { "${it.name}: ${it.value}" }}")
                    LOGGER.executionProgressLine("Solve Outputs"," - ${step.outputs.joinToString("\n - ") { "${it.name}: ${it.value}" }}")
                } else {
                    LOGGER.failedExecutionMinor("Solve Failed: ${step.inputs.joinToString("\n") { "$it" }}")
                }
                state.solveHistory.add(step)

                // 4. Finally, check if the workflow has been completed
                state.checkDone()
                if (!state.isDone)
                    LOGGER.executionComment("Workflow execution not yet complete.")
                state.printTaskPlan(listOf(state.taskTree))
            }
            if (i <= maxSteps) {
                val execTime = System.currentTimeMillis() - t0
                LOGGER.execution("Workflow execution complete in ${execTime}ms and ${i-1} steps!")
                LOGGER.execution("Final Result:")
                LOGGER.executionStep(state.finalResult())
            }
        }
        return state
    }

}

/** Manages logging for [WorkflowExecutor]. */
private object WorkflowLogger {
    var isEnabled = true

    fun failedExecutionMajor(msg: Any) = println("$ANSI_RED$msg$ANSI_RESET")
    fun failedExecutionMinor(msg: Any) = println("$ANSI_RED$msg$ANSI_RESET")

    fun executionStep(msg: Any) = println("$ANSI_GREEN$msg$ANSI_RESET")
    fun execution(msg: Any) = println(msg)
    fun executionComment(msg: Any) = println("$ANSI_GRAY$msg$ANSI_RESET")
    fun executionProgress(key: String, value: Any) = println("$key: $ANSI_CYAN$value$ANSI_RESET")
    fun executionProgressLine(key: String, value: Any) = println("$key:\n$ANSI_CYAN$value$ANSI_RESET")

    fun println(string: String) {
        if (isEnabled)
            kotlin.io.println(string)
    }
}

