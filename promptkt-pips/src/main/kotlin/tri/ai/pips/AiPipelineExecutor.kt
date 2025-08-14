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
package tri.ai.pips

import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiPromptTraceSupport

/** Pipeline for chaining together collection of tasks to be accomplished by AI or APIs. */
object AiPipelineExecutor {

    /** More robust execution, allowing for retry of failed attempts. */
    private val executor = RetryExecutor()

    /**
     * Execute tasks in order, chaining results from one to another.
     * Returns the table of execution results.
     */
    suspend fun execute(tasks: List<AiTask<*>>, monitor: AiTaskMonitor): AiPipelineResult<*> {
        require(tasks.isNotEmpty())

        val completedTasks = mutableMapOf<String, AiPromptTraceSupport<*>>()
        val failedTasks = mutableMapOf<String, AiPromptTraceSupport<*>>()

        var tasksToDo: List<AiTask<*>>
        do {
            tasksToDo = tasks.filter {
                it.id !in completedTasks && it.id !in failedTasks
            }.filter {
                it.dependencies.all { it in completedTasks && completedTasks[it]!!.exec.succeeded() }
            }
            tasksToDo.forEach { task ->
                try {
                    monitor.taskStarted(task)
                    val input = task.dependencies.associateWith { completedTasks[it]!! }
                    val result = executor.execute(task, input, monitor)
                    val resultValue = result.output?.outputs
                    val err = result.exec.throwable ?: (if (resultValue == null) IllegalArgumentException("No value") else null)
                    if (err != null) {
                        monitor.taskFailed(task, err)
                        failedTasks[task.id] = result
                    } else {
                        monitor.taskCompleted(task, resultValue)
                        completedTasks[task.id] = result
                    }
                } catch (x: Exception) {
                    x.printStackTrace()
                    monitor.taskFailed(task, x)
                    failedTasks[task.id] = AiPromptTrace.error<Any>(null, x.message!!, x)
                }
            }
        } while (tasksToDo.isNotEmpty())

        val allTasks = completedTasks + failedTasks
        val lastTaskResult = allTasks[tasks.last().id] ?: AiPromptTrace.error<Any>(null, "Inputs failed.")
        return AiPipelineResult(lastTaskResult, completedTasks + failedTasks)
    }

}

