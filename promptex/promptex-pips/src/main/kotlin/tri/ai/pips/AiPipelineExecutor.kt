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
import tri.ai.core.tool.ExecContext
import tri.ai.prompt.trace.AiOutput
import tri.ai.prompt.trace.AiOutputInfo
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
    suspend fun execute(tasks: List<AiTask<*, *>>, monitor: FlowCollector<ExecEvent>): AiPipelineResult {
        require(tasks.isNotEmpty()) { "No tasks to execute." }

        val completedTasks = mutableMapOf<String, AiPromptTraceSupport>()
        val failedTasks = mutableMapOf<String, AiPromptTraceSupport>()
        val completedOutputs = mutableMapOf<String, Any?>()

        var tasksToDo: List<AiTask<*, *>>
        do {
            tasksToDo = tasks.filter {
                it.id !in completedTasks && it.id !in failedTasks
            }.filter {
                it.dependencies.all { it in completedTasks && completedTasks[it]!!.exec.succeeded() }
            }
            tasksToDo.forEach { task ->
                try {
                    monitor.emitTaskStarted(task)
                    val taskInputs = task.dependencies.associateWith { completedOutputs[it] }
                    val input = if (task.dependencies.size == 1) completedOutputs[task.dependencies.first()] else null
                    val context = ExecContext(monitor = monitor, taskInputs = taskInputs)
                    val output = executor.execute(task, input, context)
                    // Resolve trace: prefer context.traces (new-style), then check if result is a trace (legacy)
                    val trace: AiPromptTraceSupport = context.traces[task.id]
                        ?: (output as? AiPromptTraceSupport)
                        ?: if (output != null) AiPromptTrace(outputInfo = toOutputInfo(output))
                           else AiPromptTrace(outputInfo = null)
                    val resultValue = trace.output?.outputs
                    val err = trace.exec.throwable ?: (if (resultValue == null) IllegalArgumentException("No value") else null)
                    if (err != null) {
                        monitor.emitTaskFailed(task, err)
                        failedTasks[task.id] = trace
                    } else {
                        monitor.emitTaskCompleted(task, resultValue)
                        completedTasks[task.id] = trace
                        completedOutputs[task.id] = output
                    }
                } catch (x: Exception) {
                    x.printStackTrace()
                    monitor.emitTaskFailed(task, x)
                    failedTasks[task.id] = AiPromptTrace.error(null, x.message!!, x)
                }
            }
        } while (tasksToDo.isNotEmpty())

        val allTasks = completedTasks + failedTasks
        val lastTaskResult = allTasks[tasks.last().id] ?: AiPromptTrace.error(null, "Inputs failed.")
        return AiPipelineResult(lastTaskResult, completedTasks + failedTasks)
    }

}

/** Converts a plain task output value to [AiOutputInfo] for use in a synthetic trace. */
private fun toOutputInfo(output: Any): AiOutputInfo = when (output) {
    is List<*> -> AiOutputInfo.output(AiOutput(other = output))
    else -> AiOutputInfo.other(output)
}

