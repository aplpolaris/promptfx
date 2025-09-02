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
import tri.util.*

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

}

/** Helper to create an [AgentChatFlow] from a flow builder. */
fun agentflow(block: suspend FlowCollector<AgentChatEvent>.() -> Unit) =
    AgentChatFlow(flow(block))

/** Events emitted during an agent chat operation. */
sealed class AgentChatEvent {
    /** Progress update during processing. */
    data class Progress(val message: String) : AgentChatEvent()
    /** Interim reasoning/thought process. */
    data class Reasoning(val reasoning: String) : AgentChatEvent()
    /** Tool invocation. */
    data class UsingTool(val toolName: String, val input: String) : AgentChatEvent()
    /** Streaming token from response generation. */
    data class StreamingToken(val token: String) : AgentChatEvent()
    /** Final response from the agent. */
    data class Response(val response: AgentChatResponse) : AgentChatEvent()
    /** Error occurred during processing. */
    data class Error(val error: Throwable) : AgentChatEvent()
}

/** A basic collector that prints all events to standard out. */
class AgentFlowLogger(var verbose: Boolean = false) : FlowCollector<AgentChatEvent> {

    override suspend fun emit(event: AgentChatEvent) {
        when (event) {
            is AgentChatEvent.Progress -> printlnProgress(event.message)
            is AgentChatEvent.Reasoning -> printlnThought(event.reasoning)
            is AgentChatEvent.UsingTool -> printlnTool("${event.toolName} (${event.input})")
            is AgentChatEvent.StreamingToken -> print(event.token)
            is AgentChatEvent.Response -> {
                val responseText = event.response.message.content?.firstOrNull()?.text ?: "[No response]"
                // TODO - if have been printing intermediate tokens, may not need to print the full response
                printlnResponse("[Response] $responseText")

                if (event.response.reasoning != null) {
                    printlnProgress("[Reasoning] ${event.response.reasoning}")
                }
            }
            is AgentChatEvent.Error -> {
                printlnError(event.error.message)
                if (verbose) {
                    event.error.printStackTrace()
                }
            }
        }
    }

    private fun printlnResponse(text: String) {
        println("${ANSI_LIGHTBLUE}$text$ANSI_RESET")
    }

    private fun printlnProgress(text: String) {
        println("$ANSI_BLUISH_GRAY[Progress] $text$ANSI_RESET")
    }

    private fun printlnThought(text: String) {
        println("$ANSI_LIGHTGREEN[Thought]  ${text.replace("\n","\n           ")}$ANSI_RESET")
    }

    private fun printlnTool(text: String) {
        println("$ANSI_ORANGE[Tool]     $text$ANSI_RESET")
    }

    private fun printlnError(text: String?) {
        println("${ANSI_RED}ERROR: $text$ANSI_RESET")
    }

}