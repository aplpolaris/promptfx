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
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.TextChat
import tri.ai.core.agent.AgentChatConfig
import tri.ai.core.agent.AgentChatSession
import tri.ai.core.agent.createObject
import tri.ai.core.textContent
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.Executable
import tri.ai.core.agent.wf.WorkflowExecutorChat
import tri.ai.core.agent.wf.WorkflowExecutor
import tri.ai.core.agent.wf.WorkflowSolveStep
import tri.ai.core.agent.wf.WorkflowSolver
import tri.ai.core.agent.wf.WorkflowState
import tri.ai.core.agent.wf.WorkflowTask

/**
 * An agent-based executable unit in a Pips pipeline, built using plan-and-act logic with a set of tools.
 * Uses [WorkflowExecutor] to decompose and execute tasks.
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
        val request = MultimodalChatMessage.user(input.get("request")?.asText() ?: input.toString())
        
        // Get completion service from context resources
        val textChatResource = context.resources["textChat"]
        val textChatId = (textChatResource as? TextChat)?.modelId ?: textChatResource as? String
            ?: throw IllegalArgumentException("Text completion service not found in context resources or invalid: $textChatResource")
            
        val execStrategy = WorkflowExecutorChat(AgentChatConfig(modelId = textChatId, maxTokens = 2000, temperature = 0.5))
        val solvers = tools.map { it.toSolver(context) }

        val executor = WorkflowExecutor(execStrategy, solvers)
        val finalState = executor.sendMessage(AgentChatSession(), request).awaitResponse()

        return createObject("result", finalState.message.textContent())
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
        
        val inputJson = createObject("input", input)
        val resultJson = this@toSolver.execute(inputJson, context)
        val result = resultJson.get("result")?.asText() ?: resultJson.toString()
        
        val tt = System.currentTimeMillis() - t0
        return solveStep(task, inputs(input), outputs(result), tt, true)
    }
}
