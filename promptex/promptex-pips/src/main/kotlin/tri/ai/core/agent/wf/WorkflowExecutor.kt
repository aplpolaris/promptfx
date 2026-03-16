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
import tri.ai.core.tool.ExecContext
import tri.ai.pips.ExecEvent
import tri.ai.pips.emitError
import tri.ai.pips.emitPlanningTask
import tri.ai.pips.emitProgress
import tri.ai.pips.emitToolResult
import tri.ai.pips.emitUsingTool
import tri.ai.core.agent.AgentChatResponse
import tri.ai.core.agent.AgentChatSession
import tri.ai.core.agent.AgentChatSupport
import tri.ai.pips.SimpleRetryExecutor
import tri.util.json.createObject

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
    override suspend fun FlowCollector<ExecEvent>.sendMessageSafe(session: AgentChatSession, message: MultimodalChatMessage): AgentChatResponse {
        val question = logTextContent(message)
        updateSession(message, session)
        logToolUsage()

        val response = solve(WorkflowUserRequest(question))

        // store and log response, maybe update session name
        updateSession(response, session, updateName = true)
        return agentChatResponse(response, session)
    }

    /** Logs tool usage. */
    private suspend fun FlowCollector<ExecEvent>.logToolUsage() {
        emitProgress("Using solvers: [${solvers.joinToString(", ") { it.name }}]")
    }

    private suspend fun FlowCollector<ExecEvent>.solve(problem: WorkflowUserRequest): MultimodalChatMessage {
        val planState = WorkflowPlanState(problem)
        val context = ExecContext(monitor = this)
        context.putResource(RESOURCE_WORKFLOW_PLAN_STATE, planState)
        context.initWorkflowContext()
        emitProgress("Workflow Planning...")

        var i = 1
        val t0 = System.currentTimeMillis()
        while (!planState.isDone) {
            if (i > maxSteps) {
                emitError(Exception("Too many steps, stopping execution."))
                break
            }

            // 1. Do a planning step, which may involve breaking up a task into smaller tasks
            val execResult: Any = SimpleRetryExecutor().execute(
                { strategy.decomposeTask(planState, solvers) },
                onSuccess = { it.value!! },
                onFailure = { it.error!! }
            )

            if (execResult is Exception) {
                emitError(execResult)
                break
            }
            check(execResult is WorkflowTaskPlan) { "Expected WorkflowTaskPlan from decomposeTask, got ${execResult::class}" }
            if (execResult.decomp.isNotEmpty()) {
                planState.updateTasking(execResult)
                emitProgress(planState.printTaskPlan(listOf(planState.taskTree)))
            }

            // 2. Select a solver and task to work on
            emitProgress("Workflow Step ${i++}")
            val (solver, task) = strategy.nextSolver(planState, solvers)
            emitPlanningTask(task.id, task.name + (task.description?.let { " - $it" } ?: ""))

            // 3. Use the selected solver to execute part of the task
            context.putResource(RESOURCE_WORKFLOW_TASK, task)
            val t0step = System.currentTimeMillis()
            val inputJson = createObject(INPUT, context.aggregateWorkflowInputsAsStringFor(solver.name, task.name))
            emitUsingTool(solver.name, inputJson.prettyPrint())
            val outputJson = solver.execute(inputJson, context)
            val step = WorkflowSolveStep(task, solver, inputJson, outputJson as com.fasterxml.jackson.databind.node.ObjectNode, System.currentTimeMillis() - t0step, true)

            planState.taskTree.setTaskDone(task)
            context.addWorkflowResults(task, step.outputs)
            emitToolResult(solver.name, step.outputs.prettyPrint())
            planState.solveHistory.add(step)

            // 4. Finally, check if the workflow has been completed
            planState.checkDone()
            emitProgress(planState.printTaskPlan(listOf(planState.taskTree)))
            if (!planState.isDone)
                emitProgress("Continuing workflow execution...")
        }
        if (i <= maxSteps) {
            val execTime = System.currentTimeMillis() - t0
            emitProgress("Workflow execution complete in ${execTime}ms and ${i-1} steps!")
            val final = context.workflowFinalResult().prettyPrint()
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

