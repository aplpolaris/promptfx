/*-
 * #%L
 * tri.promptfx:promptkt
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

import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiPromptTraceSupport

/**
 * Task that can be executed by AI or API.
 * A task may have an arbitrary number of inputs that must be calculated prior to the task being executable.
 * The types of these inputs are not specified.
 */
abstract class AiTask<T>(
    val id: String,
    val description: String? = null,
    val dependencies: Set<String> = setOf()
) {
    /**
     * A task that can be executed, using a table of input results indexed by key.
     * Input types are not specified.
     */
    abstract suspend fun execute(inputs: Map<String, AiPromptTraceSupport<*>>, monitor: AiTaskMonitor): AiPromptTraceSupport<T>

    /** Wrap this in a task that monitors and informs a callback when result is obtained. */
    fun monitor(callback: (List<T>) -> Unit): AiTask<T> = object : AiTask<T>(id) {
        override suspend fun execute(inputs: Map<String, AiPromptTraceSupport<*>>, monitor: AiTaskMonitor): AiPromptTraceSupport<T> {
            val res = this@AiTask.execute(inputs, monitor)
            res.outputInfo?.outputs?.let { callback(it) }
            return res
        }
    }
    /** Wrap this in a task that monitors and informs a callback when result is obtained. */
    fun monitorTrace(callback: (AiPromptTraceSupport<T>) -> Unit): AiTask<T> = object : AiTask<T>(id) {
        override suspend fun execute(inputs: Map<String, AiPromptTraceSupport<*>>, monitor: AiTaskMonitor): AiPromptTraceSupport<T> {
            val res = this@AiTask.execute(inputs, monitor)
            callback(res)
            return res
        }
    }

    companion object {
        /** Creates a task. */
        fun <T> task(id: String, description: String? = null, op: suspend () -> T): AiTask<T> =
            aitask(id, description) {
                val res = op()
                if (res is AiPromptTraceSupport<*>) throw IllegalArgumentException("Use aitask() for AiPromptTraceSupport")
                AiPromptTrace.result(res, modelId = null)
            }

        /** Creates a task. */
        fun <T> aitask(id: String, description: String? = null, op: suspend () -> AiPromptTraceSupport<T>): AiTask<T> =
            object: AiTask<T>(id, description) {
                override suspend fun execute(inputs: Map<String, AiPromptTraceSupport<*>>, monitor: AiTaskMonitor) = op()
            }

        /** Creates a task that depends on a provided list of tasks. */
        fun <T> List<AiTask<*>>.task(id: String, description: String? = null, op: suspend (Map<String, AiPromptTraceSupport<*>>) -> T): AiTask<T> =
            aitask(id, description) { AiPromptTrace.result(op(it)) }

        /** Creates a task that depends on a provided list of tasks. */
        fun <T> List<AiTask<*>>.aitask(id: String, description: String? = null, op: suspend (Map<String, AiPromptTraceSupport<*>>) -> AiPromptTraceSupport<T>): AiTask<T> =
            object : AiTask<T>(id, description, map { it.id }.toSet()) {
                override suspend fun execute(inputs: Map<String, AiPromptTraceSupport<*>>, monitor: AiTaskMonitor) =
                    op(inputs)
            }
    }
}

