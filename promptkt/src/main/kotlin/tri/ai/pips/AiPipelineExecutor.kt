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

/** Pipeline for chaining together collection of tasks to be accomplished by AI or APIs. */
object AiPipelineExecutor {

    /**
     * Execute tasks in order, chaining results from one to another.
     * Returns the table of execution results.
     */
    suspend fun execute(tasks: List<AiTask<*>>, monitor: AiTaskMonitor): AiPipelineResult {
        require(tasks.isNotEmpty())

        val completedTasks = mutableMapOf<String, AiTaskResult<*>>()
        val failedTasks = mutableMapOf<String, AiTaskResult<*>>()

        var tasksToDo: List<AiTask<*>>
        do {
            tasksToDo = tasks.filter {
                it.id !in completedTasks && it.id !in failedTasks
            }.filter {
                it.dependencies.all { it in completedTasks && completedTasks[it]!!.error == null }
            }
            tasksToDo.forEach {
                try {
                    monitor.taskStarted(it)
                    val input = it.dependencies.associateWith { completedTasks[it]!! }
                    val result = it.execute(input, monitor)
                    if (result.error == null) {
                        monitor.taskCompleted(it, result.value)
                        completedTasks[it.id] = result
                    } else {
                        monitor.taskFailed(it, result.error)
                        failedTasks[it.id] = result
                    }
                } catch (x: Exception) {
                    x.printStackTrace()
                    monitor.taskFailed(it, x)
                    failedTasks[it.id] = AiTaskResult.error(x.message!!, x)
                }
            }
        } while (tasksToDo.isNotEmpty())

        return AiPipelineResult(tasks.last().id, completedTasks + failedTasks)
    }

}

