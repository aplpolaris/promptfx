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
package tri.ai.pips.agent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first

/** Events emitted during an agent chat operation. */
sealed class AgentChatEvent {
    /** Progress update during processing. */
    data class Progress(val message: String) : AgentChatEvent()
    /** Interim reasoning/thought process. */
    data class Reasoning(val reasoning: String) : AgentChatEvent()
    /** Streaming token from response generation. */
    data class StreamingToken(val token: String) : AgentChatEvent()
    /** Final response from the agent. */
    data class Response(val response: AgentChatResponse) : AgentChatEvent()
    /** Error occurred during processing. */
    data class Error(val error: Throwable) : AgentChatEvent()
}

/** 
 * Represents an ongoing agent chat operation that can be monitored for progress.
 * Allows streaming of interim results, reasoning, and progress updates.
 */
class AgentChatOperation(
    /** Stream of events from the operation. */
    val events: Flow<AgentChatEvent>
) {
    /** 
     * Await the final response from the operation.
     * This suspends until a Response event is emitted.
     */
    suspend fun awaitResponse(): AgentChatResponse {
        return events.filterIsInstance<AgentChatEvent.Response>().first().response
    }
}