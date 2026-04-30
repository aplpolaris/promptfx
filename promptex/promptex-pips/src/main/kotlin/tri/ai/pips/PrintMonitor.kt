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
import tri.ai.prompt.trace.AiOutput
import tri.ai.prompt.trace.AiTaskTrace
import tri.util.info
import tri.util.warning

/** Task monitor that prints task lifecycle events to the console. */
class PrintMonitor : FlowCollector<ExecEvent> {

    override suspend fun emit(value: ExecEvent) {
        when (value) {
            is ExecEvent.TaskStarted -> printGray("Started: ${value.task.id}")
            is ExecEvent.TaskUpdate -> printGray("Update: ${value.task.id} ${value.progress}")
            is ExecEvent.TaskCompleted -> {
                val result = value.result
                val v = (result as? AiTaskTrace)?.output?.outputs ?: result
                if (v is Iterable<*> && v.count() > 1) {
                    printGray("  result:")
                    v.forEach { printGray("\u001B[1m    - ${it.pretty()}") }
                } else if (v is Iterable<*> && v.count() == 1) {
                    printGray("  result: \u001B[1m${v.first().pretty()}")
                } else {
                    printGray("  result: \u001B[1m${v.pretty()}")
                }
                printGray("  completed: ${value.task.id}")
            }
            is ExecEvent.TaskFailed -> printRed("  failed: ${value.task.id} with error ${value.error}")
            else -> {} // ignore agent/chat events
        }
    }

    private fun Any?.pretty(): String = when (this) {
        null -> "null"
        is Unit -> "✓"
        is AiTaskTrace -> output?.outputs?.let { if (it.size == 1) it[0].pretty() else it.joinToString(", ") { it.pretty() } } ?: "null"
        is AiOutput.Text -> text
        is AiOutput.ChatMessage -> message.content ?: "(no message content)"
        is AiOutput.MultimodalMessage -> textContent(ifNone = "(multimodal output)")
        is AiOutput.Other -> runCatching { other.toString() }.getOrElse { "(${other::class.simpleName})" }
        else -> toString()
    }

    /** Hook for printing start of simple tasks. */
    fun taskStarted(id: String) {
        printGray("Started: $id")
    }

    /** Hook for printing completion of simple tasks. */
    fun taskCompleted(id: String) {
        printGray("  completed: $id")
    }

    private fun printGray(text: String) {
        info<PrintMonitor>("\u001B[90m$text\u001B[0m")
    }

    private fun printRed(text: String) {
        warning<PrintMonitor>("\u001B[91m$text\u001B[0m")
    }
}
