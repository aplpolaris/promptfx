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
package tri.ai.pips.api

import com.fasterxml.jackson.databind.JsonNode
import tri.ai.core.textContent
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.Executable
import tri.ai.tool.wf.WExecutorChat
import tri.ai.tool.wf.WorkflowExecutor
import tri.ai.tool.wf.WorkflowSolveStep
import tri.ai.tool.wf.WorkflowSolver
import tri.ai.tool.wf.WorkflowState
import tri.ai.tool.wf.WorkflowTask
import tri.ai.tool.wf.WorkflowUserRequest

/**
 * An agent-based executable unit in a Pips pipeline, built using plan-and-act logic with a set of tools.
 * Uses [tri.ai.tool.wf.WorkflowExecutor] to decompose and execute tasks.
 *
 * input: { "request" : "user request text" }
 * output: { "result" : "final result text" }
 */
class AgentExecutable(
    override val name: String,
    override val description: String,
    override val version: String,
    override val inputSchema: JsonNode?,
    override val outputSchema: JsonNode?,
    val tools: List<Executable>
) : Executable {

    override suspend fun execute(
        input: JsonNode,
        context: ExecContext
    ): JsonNode {
        val request = WorkflowUserRequest(input.get("request")?.asText() ?: input.toString())
        
        // Get completion service from context resources
        val textCompletion = context.resources["textCompletion"] as? tri.ai.core.TextCompletion
            ?: throw IllegalArgumentException("Text completion service not found in context resources")
            
        val execStrategy = WExecutorChat(textCompletion, maxTokens = 2000, temp = 0.5)
        val solvers = tools.map { it.toSolver(context) }

        val executor = WorkflowExecutor(execStrategy, solvers)
        val finalState = executor.solve(request).awaitResponse()

        return context.mapper.createObjectNode().put("result", finalState.message.textContent())
    }

}

/** Converts an [Executable] to a [WorkflowSolver]. */
private fun Executable.toSolver(context: ExecContext) = object : WorkflowSolver(
    name, 
    description, 
    mapOf("input" to "Input for $name"), 
    mapOf("result" to "Result from $name")
) {
    override suspend fun solve(
        state: WorkflowState,
        task: WorkflowTask
    ): WorkflowSolveStep {
        val t0 = System.currentTimeMillis()
        val input = state.aggregateInputsFor(name).values.mapNotNull { it?.value }.ifEmpty {
            listOf(task.name)
        }.joinToString("\n")
        
        val inputJson = context.mapper.createObjectNode().put("input", input)
        val resultJson = this@toSolver.execute(inputJson, context)
        val result = resultJson.get("result")?.asText() ?: resultJson.toString()
        
        val tt = System.currentTimeMillis() - t0
        return solveStep(task, inputs(input), outputs(result), tt, true)
    }
}
