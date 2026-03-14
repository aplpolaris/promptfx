/*-
 * #%L
 * tri.promptfx:promptex-pips
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
package tri.ai.core.agent

import kotlinx.coroutines.flow.FlowCollector
import tri.ai.pips.ExecEvent

/**
 * A recorder that captures all events from an [AgentChatFlow] and can print
 * a summary of the tool calls and reasoning steps after the workflow completes.
 */
class AgentFlowRecorder : FlowCollector<ExecEvent> {

    /** All collected events, in the order they were emitted. */
    val events = mutableListOf<ExecEvent>()

    override suspend fun emit(event: ExecEvent) {
        events.add(event)
    }

    /** Print a structured summary of captured tool calls and reasoning steps to standard out. */
    fun printSummary() {
        val planningTasks = events.filterIsInstance<ExecEvent.PlanningTask>()
        val reasoningSteps = events.filterIsInstance<ExecEvent.Reasoning>()
        val toolCallEvents = events.filterIsInstance<ExecEvent.UsingTool>()
        val toolResultEvents = events.filterIsInstance<ExecEvent.ToolResult>()
        val response = events.filterIsInstance<ExecEvent.Response>().lastOrNull()

        println("=== Workflow Summary ===")

        if (planningTasks.isNotEmpty()) {
            println("Planning Tasks (${planningTasks.size}):")
            planningTasks.forEach { task ->
                println("  [${task.taskId}] ${task.description}")
            }
        }

        if (reasoningSteps.isNotEmpty()) {
            println("Reasoning Steps (${reasoningSteps.size}):")
            reasoningSteps.forEachIndexed { index, step ->
                val firstLine = step.reasoning.trim().lines().first()
                println("  ${index + 1}. $firstLine")
            }
        }

        if (toolCallEvents.isNotEmpty()) {
            println("Tool Calls (${toolCallEvents.size}):")
            // Match each tool call to its result by scanning events in order (FIFO per tool name)
            val pendingResults = toolResultEvents.toMutableList()
            toolCallEvents.forEach { call ->
                val resultIdx = pendingResults.indexOfFirst { it.toolName == call.toolName }
                val result = if (resultIdx >= 0) pendingResults.removeAt(resultIdx) else null
                val inputSummary = call.input.trim().lines().first()
                println("  Tool: ${call.toolName}")
                println("    Input:  $inputSummary")
                if (result != null) {
                    val resultSummary = result.result.trim().lines().first()
                    println("    Result: $resultSummary")
                }
            }
        }

        if (response != null) {
            val responseText = response.response.message.content?.firstOrNull()?.text ?: "[No response]"
            println("Final Response:")
            responseText.trim().lines().forEach { println("  $it") }
        }

        println("=== End Summary ===")
    }
}
