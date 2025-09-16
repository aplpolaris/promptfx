/*-
 * #%L
 * tri.promptfx:promptkt-pips
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
package tri.ai.core.agent

import kotlinx.coroutines.flow.*
import tri.ai.core.textContent

/**
 * Represents an ongoing agent chat operation that can be monitored for progress.
 * Allows streaming of interim results, reasoning, and progress updates.
 */
class AgentChatFlow(val events: Flow<AgentChatEvent>) {
    /**
     * Await the final response from the operation.
     * This suspends until a Response event is emitted.
     */
    suspend fun awaitResponse() =
        events.filterIsInstance<AgentChatEvent.Response>().first().response

    /**
     * Await the final response from the operation, while logging all events to the console.
     */
    suspend fun awaitResponseWithLogging() =
        events
        .onEach { event -> AgentFlowLogger().emit(event) }
        .filterIsInstance<AgentChatEvent.Response>().first()
        .response
}

suspend fun FlowCollector<AgentChatEvent>.emitUser(message: String) = emit(AgentChatEvent.User(message))
suspend fun FlowCollector<AgentChatEvent>.emitProgress(message: String) = emit(AgentChatEvent.Progress(message))
suspend fun FlowCollector<AgentChatEvent>.emitReasoning(reasoning: String) = emit(AgentChatEvent.Reasoning(reasoning))
suspend fun FlowCollector<AgentChatEvent>.emitPlanningTask(taskId: String, description: String) = emit(AgentChatEvent.PlanningTask(taskId, description))
suspend fun FlowCollector<AgentChatEvent>.emitUsingTool(toolName: String, input: String) = emit(AgentChatEvent.UsingTool(toolName, input))
suspend fun FlowCollector<AgentChatEvent>.emitToolResult(toolName: String, result: String) = emit(AgentChatEvent.ToolResult(toolName, result))
suspend fun FlowCollector<AgentChatEvent>.emitStreamingToken(token: String) = emit(AgentChatEvent.StreamingToken(token))
suspend fun FlowCollector<AgentChatEvent>.emitResponse(response: AgentChatResponse) = emit(AgentChatEvent.Response(response))
suspend fun FlowCollector<AgentChatEvent>.emitError(error: Throwable) = emit(AgentChatEvent.Error(error))

/** Events emitted during an agent chat operation. */
sealed class AgentChatEvent {
    /** User message received. */
    data class User(val message: String) : AgentChatEvent()
    /** Progress update during processing. */
    data class Progress(val message: String) : AgentChatEvent()
    /** Interim reasoning/thought process. */
    data class Reasoning(val reasoning: String) : AgentChatEvent()
    /** Task planning. */
    data class PlanningTask(val taskId: String, val description: String) : AgentChatEvent()
    /** Tool invocation. */
    data class UsingTool(val toolName: String, val input: String) : AgentChatEvent()
    /** Result from a tool invocation. */
    data class ToolResult(val toolName: String, val result: String) : AgentChatEvent()
    /** Streaming token from response generation. */
    data class StreamingToken(val token: String) : AgentChatEvent()
    /** Final response from the agent. */
    data class Response(val response: AgentChatResponse) : AgentChatEvent()
    /** Error occurred during processing. */
    data class Error(val error: Throwable) : AgentChatEvent()
}