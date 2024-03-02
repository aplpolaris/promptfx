/*-
 * #%L
 * promptkt-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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

/** Task monitor that prints to console. */
class PrintMonitor: AiTaskMonitor {

    override fun taskStarted(task: AiTask<*>) {
        printGray("Started: ${task.id}")
    }

    override fun taskUpdate(task: AiTask<*>, progress: Double) {
        printGray("Update: ${task.id} $progress")
    }

    override fun taskCompleted(task: AiTask<*>, result: Any?) {
        val value = (result as? AiTaskResult<*>)?.value ?: result
        if (value is Iterable<*>) {
            printGray("  result:")
            value.forEach { printGray("\u001B[1m    - $it") }
        } else {
            printGray("  result: \u001B[1m$value")
        }
        printGray("  completed: ${task.id}")
    }

    /** Hook for printing completion of simple tasks. */
    fun taskCompleted(id: String) {
        printGray("  completed: $id")
    }

    override fun taskFailed(task: AiTask<*>, error: Throwable) {
        printRed("  failed: ${task.id} with error $error")
    }

    private fun printGray(text: String) {
        println("\u001B[90m$text\u001B[0m")
    }

    private fun printRed(text: String) {
        println("\u001B[91m$text\u001B[0m")
    }
}
