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

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.flow.FlowCollector
import tri.ai.core.MChatRole
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.agent.AgentChatEvent
import tri.ai.core.agent.AgentChatResponse
import tri.ai.core.agent.AgentChatSession
import tri.ai.core.agent.AgentChatSupport
import tri.ai.pips.SimpleRetryExecutor

/**
 * Dynamic workflow executor, works by handing off execution to various solvers, which might perform task decomposition
 * steps and/or building up a solution. Solvers are also responsible for determining if the task is "done".
 */
class WorkflowExecutor(
    val strategy: WorkflowExecutorStrategy,
    val solvers: List<WorkflowSolver>
): AgentChatSupport() {
    private var maxSteps = 8

    /** Solve the given problem using a dynamic workflow execution. */
    override suspend fun FlowCollector<AgentChatEvent>.sendMessageSafe(session: AgentChatSession, message: MultimodalChatMessage): AgentChatResponse {
        val question = logTextContent(message)
        updateSession(message, session)
        logToolUsage()

        val response = solve(WorkflowUserRequest(question))

        // store and log response, maybe update session name
        updateSession(response, session, updateName = true)
        return agentChatResponse(response, session)
    }

    /** Logs tool usage. */
    suspend fun FlowCollector<AgentChatEvent>.logToolUsage() {
        emit(AgentChatEvent.Progress("Using solvers: [${solvers.joinToString(", ") { it.name }}]"))
    }

    private suspend fun FlowCollector<AgentChatEvent>.solve(problem: WorkflowUserRequest): MultimodalChatMessage {
        val state = WorkflowState(problem)
        emit(AgentChatEvent.Progress("Workflow Planning..."))

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
            emit(AgentChatEvent.UsingTool(solver.name, step.inputs.prettyPrint()))

            if (step.isSuccess) {
                state.taskTree.setTaskDone(task)
                state.addResults(task, step.outputs)
                emit(AgentChatEvent.ToolResult(solver.name, step.outputs.prettyPrint()))
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
            val final = state.finalResult().prettyPrint()
            val message = MultimodalChatMessage.text(MChatRole.Assistant, final)
            return message
        } else {
            throw IllegalStateException("Workflow execution failed after $maxSteps steps.")
        }
    }

}

internal fun JsonNode.prettyPrint() = when {
    isTextual -> asText()
    properties().isEmpty() -> "None"
    else -> properties().joinToString("\n") { "${it.key}: ${it.value.asText()}" }
}

