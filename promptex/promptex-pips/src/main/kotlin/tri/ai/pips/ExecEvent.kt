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
package tri.ai.pips

import kotlinx.coroutines.flow.FlowCollector
import tri.ai.core.agent.AgentChatResponse

/**
 * Unified sealed class for all execution status events, replacing both the [AiTaskMonitor] callback
 * interface and [tri.ai.core.agent.AgentChatEvent]. Use a [FlowCollector]<[ExecEvent]> (i.e. [AiTaskMonitor])
 * to receive these events.
 */
sealed class ExecEvent {

    // Task lifecycle events (replaces AiTaskMonitor callbacks)

    /** Emitted when a task begins execution. */
    data class TaskStarted(val task: AiTask<*, *>) : ExecEvent()
    /** Emitted to report incremental progress within a task. */
    data class TaskUpdate(val task: AiTask<*, *>, val progress: Double) : ExecEvent()
    /** Emitted when a task completes successfully. */
    data class TaskCompleted(val task: AiTask<*, *>, val result: Any?) : ExecEvent()
    /** Emitted when a task fails with an error. */
    data class TaskFailed(val task: AiTask<*, *>, val error: Throwable) : ExecEvent()

    // Agent/chat events (replaces AgentChatEvent variants)

    /** User message received. */
    data class User(val message: String) : ExecEvent()
    /** Progress update during processing. */
    data class Progress(val message: String) : ExecEvent()
    /** Interim reasoning/thought process. */
    data class Reasoning(val reasoning: String) : ExecEvent()
    /** Task planning update. */
    data class PlanningTask(val taskId: String, val description: String) : ExecEvent()
    /** Tool invocation. */
    data class UsingTool(val toolName: String, val input: String) : ExecEvent()
    /** Result from a tool invocation. */
    data class ToolResult(val toolName: String, val result: String) : ExecEvent()
    /** Streaming token from response generation. */
    data class StreamingToken(val token: String) : ExecEvent()
    /** Final response from the agent. */
    data class Response(val response: AgentChatResponse) : ExecEvent()
    /** Error occurred during processing. */
    data class Error(val error: Throwable) : ExecEvent()
}

// Extension functions for emitting task lifecycle events

suspend fun FlowCollector<ExecEvent>.emitTaskStarted(task: AiTask<*, *>) = emit(ExecEvent.TaskStarted(task))
suspend fun FlowCollector<ExecEvent>.emitTaskUpdate(task: AiTask<*, *>, progress: Double) = emit(ExecEvent.TaskUpdate(task, progress))
suspend fun FlowCollector<ExecEvent>.emitTaskCompleted(task: AiTask<*, *>, result: Any?) = emit(ExecEvent.TaskCompleted(task, result))
suspend fun FlowCollector<ExecEvent>.emitTaskFailed(task: AiTask<*, *>, error: Throwable) = emit(ExecEvent.TaskFailed(task, error))

// Extension functions for emitting agent/chat events

suspend fun FlowCollector<ExecEvent>.emitUser(message: String) = emit(ExecEvent.User(message))
suspend fun FlowCollector<ExecEvent>.emitProgress(message: String) = emit(ExecEvent.Progress(message))
suspend fun FlowCollector<ExecEvent>.emitReasoning(reasoning: String) = emit(ExecEvent.Reasoning(reasoning))
suspend fun FlowCollector<ExecEvent>.emitPlanningTask(taskId: String, description: String) = emit(ExecEvent.PlanningTask(taskId, description))
suspend fun FlowCollector<ExecEvent>.emitUsingTool(toolName: String, input: String) = emit(ExecEvent.UsingTool(toolName, input))
suspend fun FlowCollector<ExecEvent>.emitToolResult(toolName: String, result: String) = emit(ExecEvent.ToolResult(toolName, result))
suspend fun FlowCollector<ExecEvent>.emitStreamingToken(token: String) = emit(ExecEvent.StreamingToken(token))
suspend fun FlowCollector<ExecEvent>.emitResponse(response: AgentChatResponse) = emit(ExecEvent.Response(response))
suspend fun FlowCollector<ExecEvent>.emitError(error: Throwable) = emit(ExecEvent.Error(error))
