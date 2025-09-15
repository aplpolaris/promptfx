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

import kotlinx.coroutines.flow.flow
import tri.ai.core.MChatRole
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.agent.AgentChatEvent
import tri.ai.core.agent.AgentChatFlow
import tri.ai.core.agent.AgentChatResponse
import tri.ai.pips.SimpleRetryExecutor

/**
 * Dynamic workflow executor, works by handing off execution to various solvers, which might perform task decomposition
 * steps and/or building up a solution. Solvers are also responsible for determining if the task is "done".
 */
class WorkflowExecutor(
    val strategy: WorkflowExecutorStrategy,
    val solvers: List<WorkflowSolver>
) {
    private var maxSteps = 8

    /** Solve the given problem using a dynamic workflow execution. */
    fun solve(problem: WorkflowUserRequest) = AgentChatFlow(flow {
        val state = WorkflowState(problem)
        emit(AgentChatEvent.Progress("Workflow Planning"))
        emit(AgentChatEvent.User(problem.request))

        var i = 1
        val t0 = System.currentTimeMillis()
        while (!state.isDone) {
            if (i > maxSteps) {
                emit(AgentChatEvent.Error(Exception("Too many steps, stopping execution.")))
                break
            }

            // 1. Do a planning step, which may involve breaking up a task into smaller tasks
            val execResult: Any = SimpleRetryExecutor().execute(
                { strategy.decomposeTask(state, solvers) },
                onSuccess = { it.value!! },
                onFailure = { it.error!! }
            )

            if (execResult is Exception) {
                emit(AgentChatEvent.Error(execResult))
                break
            }
            assert(execResult is WorkflowTaskPlan)
            if ((execResult as WorkflowTaskPlan).decomp.isNotEmpty()) {
                state.updateTasking(execResult)
                emit(AgentChatEvent.Progress(state.printTaskPlan(listOf(state.taskTree))))
            }

            // 2. Select a solver and task to work on
            emit(AgentChatEvent.Progress("Workflow Step ${i++}"))
            val (solver, task) = strategy.nextSolver(state, solvers)
            emit(AgentChatEvent.PlanningTask(task.id, task.name + (task.description?.let { " - $it" } ?: "")))

            // 3. Use the selected solver to solve part of the task
            val step = solver.solve(state, task)
            emit(AgentChatEvent.UsingTool(solver.name, if (step.inputs.isEmpty()) "None" else
                step.inputs.joinToString("\n") { "${it.name}: ${it.value}" }))

            if (step.isSuccess) {
                state.taskTree.setTaskDone(task)
                state.scratchpad.addResults(task, step.outputs)
                emit(AgentChatEvent.ToolResult(solver.name, step.outputs.joinToString("\n") { "${it.name}: ${it.value}" }))
            } else {
                emit(AgentChatEvent.Error(Exception("Step failed, see logs for details.")))
            }
            state.solveHistory.add(step)

            // 4. Finally, check if the workflow has been completed
            state.checkDone()
            emit(AgentChatEvent.Progress(state.printTaskPlan(listOf(state.taskTree))))
            if (!state.isDone)
                emit(AgentChatEvent.Progress("Continuing workflow execution..."))
        }
        if (i <= maxSteps) {
            val execTime = System.currentTimeMillis() - t0
            emit(AgentChatEvent.Progress("Workflow execution complete in ${execTime}ms and ${i-1} steps!"))
            emit(AgentChatEvent.Response(AgentChatResponse(MultimodalChatMessage.text(MChatRole.Assistant, state.finalResult().toString()))))
        }
    })

}

