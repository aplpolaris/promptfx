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
     * Adds a task to the end of the list, where invoking the task performs the given operation.
     * The input to the task is the first output from the final task in this [AiTaskList]. (Use [aitaskonlist] to apply to the full output list.)
     * Returns the result [String] directly.
     */
    fun task(id: String, description: String? = null, op: suspend (AiOutput) -> String): AiTaskList {
        val newTask = object : AiTask<Any?, String>(id, description, setOf(lastTask.id)) {
            override suspend fun execute(input: Any?, context: ExecContext): String =
                op((input ?: context.taskInputs[lastTask.id]).toAiOutput())
        }
        return AiTaskList(plan, newTask)
    }

    /**
     * Adds a task to the end of the list, where invoking the task performs the given operation.
     * The input to the task is the first output from the final task in this [AiTaskList]. (Use [taskonlist] to apply to the full output list.)
     * Returns the result directly as [T].
     * @throws ClassCastException if the output cannot be cast to the expected type
     */
    inline fun <reified S, T : Any> objtask(id: String, description: String? = null, crossinline op: suspend (S) -> T?): AiTaskList {
        val newTask = object : AiTask<Any?, T?>(id, description, setOf(lastTask.id)) {
            override suspend fun execute(input: Any?, context: ExecContext): T? =
                op((input ?: context.taskInputs[lastTask.id]).toAiOutput().content() as S)
        }
        return AiTaskList(plan, newTask)
    }

    /**
     * Adds an AI task to the end of the list, where invoking the task performs the given operation.
     * The input to the task is the first output from the final task in this [AiTaskList]. (Use [aitaskonlist] to apply to the full output list.)
     * Automatically logs the returned trace via [ExecContext.logTrace] and returns the first output value.
     * @deprecated Use [task] with [ExecContext.logTrace] to record AI trace information.
     */
    @Deprecated("Use task() with context.logTrace(id, trace) to record AI trace information.",
        level = DeprecationLevel.WARNING)
    fun aitask(id: String, description: String? = null, op: suspend (AiOutput) -> AiPromptTraceSupport): AiTaskList {
        val newTask = object : AiTask<Any?, AiOutput?>(id, description, setOf(lastTask.id)) {
            override suspend fun execute(input: Any?, context: ExecContext): AiOutput? {
                val aiInput = (input ?: context.taskInputs[lastTask.id]).toAiOutput()
                val result = op(aiInput)
                context.logTrace(id, result)
                return result.output?.outputs?.firstOrNull()
            }
        }
        return AiTaskList(plan, newTask)
    }

    /**
     * Adds a task to the end of the list, where invoking the task performs the given operation.
     * The input to the task is the full list of outputs from the final task in this [AiTaskList].
     * Automatically logs the returned trace via [ExecContext.logTrace] and returns the output values.
     * @deprecated Use [task] with [ExecContext.logTrace] to record AI trace information.
     */
    @Deprecated("Use task() with context.logTrace(id, trace) to record AI trace information.",
        level = DeprecationLevel.WARNING)
    fun aitaskonlist(id: String, description: String? = null, op: suspend (List<AiOutput>) -> AiPromptTraceSupport): AiTaskList {
        val newTask = object : AiTask<Any?, List<AiOutput>>(id, description, setOf(lastTask.id)) {
            override suspend fun execute(input: Any?, context: ExecContext): List<AiOutput> {
                val raw = input ?: context.taskInputs[lastTask.id]
                val values: List<AiOutput> = when {
                    // Direct List<AiOutput> from tasklist() predecessor
                    raw is List<*> -> raw.filterIsInstance<AiOutput>()
                    // Legacy: predecessor returned AiPromptTraceSupport
                    raw is AiPromptTraceSupport -> raw.values ?: listOf()
                    else -> {
                        // Check if predecessor logged a multi-value trace (pre-populated from completedTasks)
                        val predecessorTrace = context.traces[lastTask.id]
                        when {
                            predecessorTrace != null -> predecessorTrace.values ?: listOf()
                            raw is AiOutput -> listOf(raw)
                            else -> listOf()
                        }
                    }
                }
                val result = op(values)
                context.logTrace(id, result)
                return result.values ?: listOf()
            }
        }
        return AiTaskList(plan, newTask)
    }

    //endregion

}

//region BUILDER METHODS

/** Initializes [AiTaskList] with a single task that returns an [AiOutput]. */
fun task(id: String, description: String? = null, op: suspend () -> AiOutput): AiTaskList = AiTaskList(listOf(),
    object : AiTask<Any?, AiOutput>(id, description) {
        override suspend fun execute(input: Any?, context: ExecContext) = op()
    })

/** Initializes [AiTaskList] with a single task that returns a [String]. */
fun tasktext(id: String, description: String? = null, op: suspend () -> String): AiTaskList = AiTaskList(listOf(),
    object : AiTask<Any?, String>(id, description) {
        override suspend fun execute(input: Any?, context: ExecContext) = op()
    })

/** Initializes [AiTaskList] with a single task that returns a list of [AiOutput]s. */
fun tasklist(id: String, description: String? = null, op: suspend () -> List<AiOutput>): AiTaskList = AiTaskList(listOf(),
    object : AiTask<Any?, List<AiOutput>>(id, description) {
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

/** Initializes [AiTaskList] with a single task that returns an [AiOutput] and has access to the [AiTaskMonitor] for reporting sub-progress. */
fun taskwithmonitor(id: String, description: String? = null, op: suspend (AiTaskMonitor) -> AiOutput): AiTaskList = AiTaskList(listOf(),
    object : AiTask<Any?, AiOutput>(id, description) {
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
                    is AiOutput -> listOf(value.content())
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

/**
 * Converts any value to an [AiOutput] for use as task input:
 * - [AiPromptTraceSupport] → extracts [AiPromptTraceSupport.firstValue]
 * - [AiOutput] → used as-is
 * - [String] → wrapped as `AiOutput(text = this)`
 * - Any other value → wrapped as `AiOutput(other = this)`
 */
@PublishedApi internal fun Any?.toAiOutput(): AiOutput = when (this) {
    is AiPromptTraceSupport -> firstValue
    is AiOutput -> this
    is String -> AiOutput(text = this)
    else -> AiOutput(other = this)
}

