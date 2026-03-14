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
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiOutput
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiPromptTraceSupport

/**
 * Task that can be executed by AI or API.
 * A task may have an arbitrary number of inputs that must be calculated prior to the task being executable.
 * The previous task output is passed as [input], previous task outputs are accessible via [ExecContext.taskInputs],
 * and the execution monitor is accessible via [ExecContext.monitor].
 * Traces produced during execution should be logged via [ExecContext.logTrace] rather than being returned.
 */
abstract class AiTask(
    val id: String,
    val description: String? = null,
    val dependencies: Set<String> = setOf()
) {
    /**
     * Executes the task with the provided input and [ExecContext], returning a plain result object.
     * For linear pipelines [input] is the output of the single predecessor task (or null if there is none).
     * For multi-dependency tasks, all predecessor outputs are available via [ExecContext.taskInputs].
     * Trace information should be recorded via [ExecContext.logTrace] instead of being embedded in the return value.
     */
    abstract suspend fun execute(input: Any?, context: ExecContext): Any?

    /** Wrap this in a task that monitors and informs a callback when result is obtained. */
    fun monitor(callback: (List<AiOutput>) -> Unit): AiTask = object : AiTask(id) {
        override suspend fun execute(input: Any?, context: ExecContext): Any? {
            val res = this@AiTask.execute(input, context)
            val trace = context.traces[id] ?: (res as? AiPromptTraceSupport)
            trace?.output?.outputs?.let { callback(it) }
            return res
        }
    }

    /** Wrap this in a task that monitors and informs a callback when result is obtained. */
    fun monitorTrace(callback: (AiPromptTraceSupport) -> Unit): AiTask = object : AiTask(id) {
        override suspend fun execute(input: Any?, context: ExecContext): Any? {
            val res = this@AiTask.execute(input, context)
            val trace = context.traces[id] ?: (res as? AiPromptTraceSupport)
            if (trace != null) callback(trace)
            return res
        }
    }

    companion object {
        /** Creates a task. */
        fun <T : Any> task(id: String, description: String? = null, op: suspend () -> T): AiTask =
            aitask(id, description) {
                val t0 = System.currentTimeMillis()
                val res = op()
                if (res is AiPromptTraceSupport) throw IllegalArgumentException("Use aitask() for AiPromptTraceSupport")
                AiPromptTrace(execInfo = AiExecInfo.durationSince(t0), outputInfo = AiOutputInfo.other(res))
            }

        /** Creates a task returning an [AiPromptTraceSupport]. */
        fun aitask(id: String, description: String? = null, op: suspend () -> AiPromptTraceSupport): AiTask =
            object: AiTask(id, description) {
                override suspend fun execute(input: Any?, context: ExecContext) = op()
            }

        /** Creates a task that depends on a provided list of tasks. */
        fun <T : Any> List<AiTask>.task(id: String, description: String? = null, op: suspend (Map<String, Any?>) -> T): AiTask =
            aitask(id, description) {
                val t0 = System.currentTimeMillis()
                val res = op(it)
                if (res is AiPromptTraceSupport) throw IllegalArgumentException("Use aitask() for AiPromptTraceSupport")
                AiPromptTrace(execInfo = AiExecInfo.durationSince(t0), outputInfo = AiOutputInfo.other(res))
            }

        /** Creates a task that depends on a provided list of tasks, returning an [AiPromptTraceSupport]. */
        fun List<AiTask>.aitask(id: String, description: String? = null, op: suspend (Map<String, Any?>) -> AiPromptTraceSupport): AiTask =
            object : AiTask(id, description, map { it.id }.toSet()) {
                override suspend fun execute(input: Any?, context: ExecContext) =
                    op(context.taskInputs)
            }
    }
}


