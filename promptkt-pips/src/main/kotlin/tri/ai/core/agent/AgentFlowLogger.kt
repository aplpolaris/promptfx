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
package tri.ai.core.agent

import kotlinx.coroutines.flow.FlowCollector
import tri.util.ANSI_BLUISH_GRAY
import tri.util.ANSI_LIGHTBLUE
import tri.util.ANSI_LIGHTGREEN
import tri.util.ANSI_ORANGE
import tri.util.ANSI_RED
import tri.util.ANSI_RESET
import tri.util.ANSI_SUN_YELLOW

/** A basic collector that prints all events to standard out. */
class AgentFlowLogger(var verbose: Boolean = false) : FlowCollector<AgentChatEvent> {

    override suspend fun emit(event: AgentChatEvent) {

        when (event) {
            is AgentChatEvent.User -> log(ANSI_SUN_YELLOW, "User", event.message)
            is AgentChatEvent.Progress -> log(ANSI_BLUISH_GRAY, "Progress", event.message)
            is AgentChatEvent.Reasoning -> log(ANSI_LIGHTGREEN, "Thought", event.reasoning)
            is AgentChatEvent.PlanningTask -> log(ANSI_ORANGE, "Task", event.taskId, event.description)
            is AgentChatEvent.UsingTool -> log(ANSI_ORANGE, "Tool-In", event.toolName, event.input)
            is AgentChatEvent.ToolResult -> log(ANSI_ORANGE, "Tool-Out", event.toolName, event.result)
            is AgentChatEvent.StreamingToken -> print(event.token)
            is AgentChatEvent.Response -> {
                val responseText = event.response.message.content?.firstOrNull()?.text ?: "[No response]"
                // TODO - if have been printing intermediate tokens, may not need to print the full response
                log(ANSI_LIGHTBLUE, "Final", responseText)

                if (event.response.reasoning != null) {
                    log(ANSI_BLUISH_GRAY, "Reasoning", event.response.reasoning)
                }
            }
            is AgentChatEvent.Error -> {
                log(ANSI_RED, "ERROR", event.error.message.toString())
                if (verbose) {
                    event.error.printStackTrace()
                }
            }
        }
    }

    private fun log(ansiColor: String, label: String, text: String, text2: String? = null) {
        val trimmed = text.trim()
        print(ansiColor + "[$label]".padEnd(12))
        val useNewLine = if (text2 == null) "\n" in trimmed else "\n" in text2.trim()
        if (!useNewLine && text2 != null)
            println("[$trimmed] $text2$ANSI_RESET")
        else if (!useNewLine)
            println("$trimmed$ANSI_RESET")
        else if (text2 == null) {
            println(trimmed.lineSequence().first())
            trimmed.lineSequence().drop(1).forEach { println(" ".repeat(12) + it) }
        } else {
            val length = trimmed.length + 3
            println("[$trimmed] " + text2.lineSequence().first())
            text2.trim().lineSequence().drop(1).forEach {
                println(" ".repeat(12 + length) + it)
            }
        }
    }

}
