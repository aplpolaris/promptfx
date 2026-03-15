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
import tri.ai.prompt.trace.*

/** Stores a list of tasks that can be executed in order. */
class AiTaskList(tasks: List<AiTask<*, *>>, val lastTask: AiTask<*, *>) {

    val plan = tasks + lastTask
    val planner
        get() = object : AiPlanner {
            override fun plan() = plan
        }

    //region BUILDER METHODS

    /**
     * Adds a task to the end of the list, where the predecessor's output is cast to [S] and the result [T] is returned directly.
     * The input to the task is the first output from the final task in this [AiTaskList].
     */
    inline fun <reified S, reified T : Any> task(id: String, description: String? = null, crossinline op: suspend (S?) -> T?): AiTaskList {
        val newTask = object : AiTask<Any?, T?>(id, description, setOf(lastTask.id)) {
            override suspend fun execute(input: Any?, context: ExecContext): T? {
                val raw = input ?: context.taskInputs[lastTask.id]
                @Suppress("UNCHECKED_CAST")
                return op(raw as? S)
            }
        }
        return AiTaskList(plan, newTask)
    }

    /**
     * Adds a task to the end of the list, where invoking the task performs the given operation.
     * The input to the task is the first output from the final task in this [AiTaskList]. (Use [taskonlist] to apply to the full output list.)
     * Returns the result directly as [T], extracting [S] from any [AiPromptTraceSupport] or [AiOutput] wrapper if needed.
     * @throws ClassCastException if the output cannot be cast to the expected type
     */
    inline fun <reified S, T : Any> objtask(id: String, description: String? = null, crossinline op: suspend (S) -> T?): AiTaskList {
        val newTask = object : AiTask<Any?, T?>(id, description, setOf(lastTask.id)) {
            override suspend fun execute(input: Any?, context: ExecContext): T? {
                val raw = input ?: context.taskInputs[lastTask.id]
                // Unwrap AiOutput/trace wrappers to extract the typed value
                @Suppress("UNCHECKED_CAST")
                val extracted: S = when (raw) {
                    is AiPromptTraceSupport -> raw.firstValue.content() as S
                    is AiOutput -> raw.content() as S
                    else -> raw as S
                }
                return op(extracted)
            }
        }
        return AiTaskList(plan, newTask)
    }

    /**
     * Adds an AI task to the end of the list, where invoking the task performs the given operation.
     * The predecessor's output is cast to [S] and passed to the lambda.
     * Automatically logs the returned trace via [ExecContext.logTrace] and returns the first output value.
     * @deprecated Use [task] with [ExecContext.logTrace] to record AI trace information.
     */
    @Deprecated("Use task() with context.logTrace(id, trace) to record AI trace information.",
        level = DeprecationLevel.WARNING)
    inline fun <reified S> aitask(id: String, description: String? = null, crossinline op: suspend (S?) -> AiPromptTraceSupport): AiTaskList {
        val newTask = object : AiTask<Any?, AiOutput?>(id, description, setOf(lastTask.id)) {
            override suspend fun execute(input: Any?, context: ExecContext): AiOutput? {
                val raw = input ?: context.taskInputs[lastTask.id]
                @Suppress("UNCHECKED_CAST")
                val aiInput = raw as? S
                val result = op(aiInput)
                context.logTrace(id, result)
                return result.output?.outputs?.firstOrNull()
            }
        }
        return AiTaskList(plan, newTask)
    }

    /**
     * Adds a task to the end of the list, where invoking the task performs the given operation.
     * The predecessor's output is collected as [List]<[S]> and passed to the lambda.
     * Automatically logs the returned trace via [ExecContext.logTrace] and returns the output values.
     * @deprecated Use [task] with [ExecContext.logTrace] to record AI trace information.
     */
    @Deprecated("Use task() with context.logTrace(id, trace) to record AI trace information.",
        level = DeprecationLevel.WARNING)
    inline fun <reified S> aitaskonlist(id: String, description: String? = null, crossinline op: suspend (List<S>) -> AiPromptTraceSupport): AiTaskList {
        val newTask = object : AiTask<Any?, List<*>>(id, description, setOf(lastTask.id)) {
            override suspend fun execute(input: Any?, context: ExecContext): List<*> {
                val raw = input ?: context.taskInputs[lastTask.id]
                val values: List<S> = when {
                    // Direct typed list from tasklist() or typed task predecessor
                    raw is List<*> -> raw.filterIsInstance<S>()
                    else -> {
                        // Check if predecessor logged a multi-value trace (pre-populated from completedTasks)
                        val predecessorTrace = context.traces[lastTask.id]
                        when {
                            predecessorTrace != null -> predecessorTrace.values?.filterIsInstance<S>() ?: listOf()
                            raw is S -> listOf(raw)
                            else -> listOf()
                        }
                    }
                }
                val result = op(values)
                context.logTrace(id, result)
                return result.values ?: emptyList<Any?>()
            }
        }
        return AiTaskList(plan, newTask)
    }

    //endregion

}

//region BUILDER METHODS

/** Initializes [AiTaskList] with a single task that returns a [String]. */
fun tasktext(id: String, description: String? = null, op: suspend () -> String): AiTaskList = AiTaskList(listOf(),
    object : AiTask<Any?, String>(id, description) {
        override suspend fun execute(input: Any?, context: ExecContext) = op()
    })

/** Initializes [AiTaskList] with a single task that returns a list of values of type [T]. */
fun <T : Any?> tasklist(id: String, description: String? = null, op: suspend () -> List<T>): AiTaskList = AiTaskList(listOf(),
    object : AiTask<Any?, List<T>>(id, description) {
        override suspend fun execute(input: Any?, context: ExecContext) = op()
    })

/**
 * Creates a sequential task list with a single AI task.
 * Automatically logs the returned trace via [ExecContext.logTrace] and returns the first output value.
 * @deprecated Use [tasktext] or [task] with [ExecContext.logTrace] to record AI trace information.
 */
@Deprecated("Use tasktext() or task() with context.logTrace(id, trace) to record AI trace information.",
    level = DeprecationLevel.WARNING)
fun aitask(id: String, description: String? = null, op: suspend () -> AiPromptTraceSupport) = AiTaskList(listOf(),
    object: AiTask<Any?, AiOutput?>(id, description) {
        override suspend fun execute(input: Any?, context: ExecContext): AiOutput? {
            val result = op()
            context.logTrace(id, result)
            return result.output?.outputs?.firstOrNull()
        }
    })

/** Initializes [AiTaskList] with a single task of return type [T] that has access to the [AiTaskMonitor] for reporting sub-progress. */
fun <T : Any?> taskwithmonitor(id: String, description: String? = null, op: suspend (AiTaskMonitor) -> T): AiTaskList = AiTaskList(listOf(),
    object : AiTask<Any?, T>(id, description) {
        override suspend fun execute(input: Any?, context: ExecContext) = op(context.monitor)
    })

/**
 * Creates a sequential task list with a single AI task that has access to the [AiTaskMonitor] for reporting sub-progress.
 * Automatically logs the returned trace via [ExecContext.logTrace] and returns the first output value.
 * @deprecated Use [taskwithmonitor] with [ExecContext.logTrace] to record AI trace information.
 */
@Deprecated("Use taskwithmonitor() with context.logTrace(id, trace) to record AI trace information.",
    level = DeprecationLevel.WARNING)
fun aitaskwithmonitor(id: String, description: String? = null, op: suspend (AiTaskMonitor) -> AiPromptTraceSupport) = AiTaskList(listOf(),
    object : AiTask<Any?, AiOutput?>(id, description) {
        override suspend fun execute(input: Any?, context: ExecContext): AiOutput? {
            val result = op(context.monitor)
            context.logTrace(id, result)
            return result.output?.outputs?.firstOrNull()
        }
    })

/**
 * Create a [AiTaskList] for a list of tasks that all return the same type, where the last task returns the list of results from individual tasks.
 * The result is a `List<List<*>>` where each inner list contains the output(s) from one predecessor task:
 * for [AiPromptTraceSupport] outputs the content of each output value is extracted;
 * for [List] outputs the list is used directly; for single values they are wrapped in a one-element list.
 * @throws IllegalArgumentException if there are duplicate task IDs
 */
fun List<AiTask<*, *>>.aggregate(): AiTaskList {
    require(map { it.id }.toSet().size == size) { "Duplicate task IDs" }
    val finalTask = object : AiTask<Any?, List<*>>("promptBatch", dependencies = map { it.id }.toSet()) {
        override suspend fun execute(input: Any?, context: ExecContext): List<*> =
            context.taskInputs.values.map { value ->
                when (value) {
                    is AiPromptTraceSupport -> value.output?.outputs?.map { it.content() } ?: listOf<Any?>()
                    is List<*> -> value
                    else -> listOf(value)
                }
            }
    }
    return AiTaskList(this, finalTask)
}

/**
 * Create a [AiTaskList] for a list of tasks that all return the same type, where the last task returns the list of results from individual tasks.
 * Collects traces from [ExecContext.traces] (pre-populated from completed tasks) rather than raw task outputs,
 * preserving full AI execution metadata. Each element in the returned list is either an [AiPromptTraceSupport]
 * (when the task logged a trace) or the raw task output (when no trace was logged).
 * @throws IllegalArgumentException if there are duplicate task IDs
 */
fun List<AiTask<*, *>>.aggregatetrace(): AiTaskList {
    require(map { it.id }.toSet().size == size) { "Duplicate task IDs" }
    val finalTask = object : AiTask<Any?, List<*>>("promptBatch", dependencies = map { it.id }.toSet()) {
        override suspend fun execute(input: Any?, context: ExecContext): List<*> =
            map { task -> context.traces[task.id] ?: context.taskInputs[task.id] }
    }
    return AiTaskList(this, finalTask)
}

//endregion

