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

import tri.ai.core.tool.ExecContext
import tri.ai.prompt.trace.AiOutput
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiPromptTraceSupport

/** Executor for chaining together a collection of tasks to be accomplished by AI or APIs. */
object AiWorkflowExecutor {

    /** More robust execution, allowing for retry of failed attempts. */
    private val executor = RetryExecutor()

    /**
     * Execute a single task, returning its result. This is a convenience wrapper around [execute] for single tasks.
     */
    suspend fun execute(task: AiTask<*, *>, context: ExecContext = ExecContext()): AiWorkflowResult =
        execute(listOf(task), context)

    /**
     * Execute tasks in order, chaining results from one to another.
     * A single [ExecContext] is created and shared across all task executions; each task's output is
     * stored via [ExecContext.put] and its trace in [ExecContext.traces] so subsequent tasks can
     * access both without requiring a new context per task.
     * Returns the table of execution results.
     */
    suspend fun execute(tasks: List<AiTask<*, *>>, context: ExecContext = ExecContext()): AiWorkflowResult {
        require(tasks.isNotEmpty()) { "No tasks to execute." }

        var tasksToDo: List<AiTask<*, *>>
        do {
            tasksToDo = tasks.filter {
                it.id !in context.traces
            }.filter {
                it.dependencies.all { depId -> context.trace(depId)?.exec?.succeeded() == true }
            }
            tasksToDo.forEach { task ->
                try {
                    context.monitor.emitTaskStarted(task)
                    val input = if (task.dependencies.size == 1) context.get(task.dependencies.first()) else null
                    val output = executor.execute(task, input, context)
                    // Resolve trace: prefer context.getTrace (set by task or RetryExecutor), then synthesize
                    val trace: AiPromptTraceSupport = context.trace(task.id)
                        ?: run {
                            check(output !is AiPromptTraceSupport) {
                                "Task '${task.id}' returned AiPromptTraceSupport directly. Use context.logTrace() instead of returning traces from execute()."
                            }
                            if (output != null) AiPromptTrace(outputInfo = toOutputInfo(output))
                            else AiPromptTrace(outputInfo = null)
                        }
                    // Always store the resolved trace in context so dependency checks can use it
                    context.logTrace(task.id, trace)
                    val resultValue = trace.output?.outputs
                    val err = trace.exec.throwable ?: (if (resultValue == null) IllegalArgumentException("No value") else null)
                    if (err != null) {
                        context.monitor.emitTaskFailed(task, err)
                    } else {
                        context.monitor.emitTaskCompleted(task, resultValue)
                        context.put(task.id, output)
                    }
                } catch (x: Exception) {
                    x.printStackTrace()
                    context.monitor.emitTaskFailed(task, x)
                    context.logTrace(task.id, AiPromptTrace.error(null, x.message ?: "Unknown error", x))
                }
            }
        } while (tasksToDo.isNotEmpty())

        val lastTaskResult = context.trace(tasks.last().id) ?: AiPromptTrace.error(null, "Inputs failed.")
        return AiWorkflowResult(lastTaskResult, context.traces.toMap())
    }

}

/** Converts a plain task output value to [AiOutputInfo] for use in a synthetic trace. */
private fun toOutputInfo(output: Any): AiOutputInfo = when (output) {
    is List<*> -> AiOutputInfo.output(AiOutput(other = output))
    else -> AiOutputInfo.other(output)
}
