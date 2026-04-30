/*-
 * #%L
 * tri.promptfx:promptkt-pips
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

import kotlinx.coroutines.flow.*
import tri.ai.pips.ExecEvent
import tri.ai.pips.emitError
import tri.ai.pips.emitProgress
import tri.ai.pips.emitPlanningTask
import tri.ai.pips.emitReasoning
import tri.ai.pips.emitResponse
import tri.ai.pips.emitStreamingToken
import tri.ai.pips.emitToolResult
import tri.ai.pips.emitUsingTool
import tri.ai.pips.emitUser

/**
 * Backward-compatibility type alias: agent chat event streams now use [ExecEvent].
 * @see ExecEvent
 */
typealias AgentChatEvent = ExecEvent

/**
 * Represents an ongoing agent chat operation that can be monitored for progress.
 * Allows streaming of interim results, reasoning, and progress updates.
 */
class AgentChatFlow(val events: Flow<ExecEvent>) {
    /**
     * Await the final response from the operation.
     * This suspends until a Response event is emitted.
     */
    suspend fun awaitResponse() =
        events.filterIsInstance<ExecEvent.Response>().first().response

    /**
     * Await the final response from the operation, while logging all events to the console.
     */
    suspend fun awaitResponseWithLogging() =
        events
        .onEach { event -> AgentEventPrinter().emit(event) }
        .filterIsInstance<ExecEvent.Response>().first()
        .response
}

