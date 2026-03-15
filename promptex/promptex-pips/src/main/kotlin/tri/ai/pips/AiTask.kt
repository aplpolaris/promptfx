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
import tri.ai.prompt.trace.AiPromptTraceSupport

/**
 * Task that can be executed by AI or API, typed by its input type [I] and output type [O].
 * A task may have an arbitrary number of inputs that must be calculated prior to the task being executable.
 * The previous task output is passed as [input], previous task outputs are accessible via [ExecContext.taskOutputs],
 * and the execution monitor is accessible via [ExecContext.monitor].
 * Traces produced during execution should be logged via [ExecContext.logTrace] rather than being returned.
 */
abstract class AiTask<in I, out O>(
    val id: String,
    val description: String? = null,
    val dependencies: Set<String> = setOf()
) {
    /**
     * Executes the task with the provided input and [ExecContext], returning a typed result.
     * For linear pipelines [input] is the output of the single predecessor task (or null if there is none).
     * For multi-dependency tasks, all predecessor outputs are available via [ExecContext.taskOutputs].
     * Trace information should be recorded via [ExecContext.logTrace] instead of being embedded in the return value.
     */
    abstract suspend fun execute(input: I, context: ExecContext): O

    /** Wrap this in a task that monitors and informs a callback when result is obtained. */
    fun monitor(callback: (List<AiOutput>) -> Unit): AiTask<I, O> = object : AiTask<I, O>(id) {
        override suspend fun execute(input: I, context: ExecContext): O {
            val res = this@AiTask.execute(input, context)
            val trace = context.traces[id] ?: (res as? AiPromptTraceSupport)
            trace?.output?.outputs?.let { callback(it) }
            return res
        }
    }

    /** Wrap this in a task that monitors and informs a callback when result is obtained. */
    fun monitorTrace(callback: (AiPromptTraceSupport) -> Unit): AiTask<I, O> = object : AiTask<I, O>(id) {
        override suspend fun execute(input: I, context: ExecContext): O {
            val res = this@AiTask.execute(input, context)
            val trace = context.traces[id] ?: (res as? AiPromptTraceSupport)
            if (trace != null) callback(trace)
            return res
        }
    }

    companion object {
        /** Creates a task that returns [T] directly. */
        fun <T : Any> task(id: String, description: String? = null, op: suspend (ExecContext) -> T) =
            object : AiTask<Void, T>(id, description) {
                override suspend fun execute(input: Void, context: ExecContext) = op(context)
            }
    }
}


