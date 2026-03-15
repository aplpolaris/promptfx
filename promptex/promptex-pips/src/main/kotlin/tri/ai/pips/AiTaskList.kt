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
     * The result is expected to be a [AiPromptTraceSupport] object with details about model, execution time, etc.
     */
    fun aitask(id: String, description: String? = null, op: suspend (AiOutput) -> AiPromptTraceSupport): AiTaskList {
        val newTask = object : AiTask<Any?, AiPromptTraceSupport>(id, description, setOf(lastTask.id)) {
            override suspend fun execute(input: Any?, context: ExecContext): AiPromptTraceSupport =
                op((input ?: context.taskInputs[lastTask.id]).toAiOutput())
        }
        return AiTaskList(plan, newTask)
    }

    /**
     * Adds a task to the end of the list, where invoking the task performs the given operation.
     * The input to the task is the full list of outputs from the final task in this [AiTaskList].
     */
    fun aitaskonlist(id: String, description: String? = null, op: suspend (List<AiOutput>) -> AiPromptTraceSupport): AiTaskList {
        val newTask = object : AiTask<Any?, AiPromptTraceSupport>(id, description, setOf(lastTask.id)) {
            override suspend fun execute(input: Any?, context: ExecContext): AiPromptTraceSupport {
                val raw = input ?: context.taskInputs[lastTask.id]
                val values: List<AiOutput> = when (raw) {
                    is AiPromptTraceSupport -> raw.values ?: listOf()
                    is List<*> -> raw.filterIsInstance<AiOutput>()
                    is AiOutput -> listOf(raw)
                    else -> listOf()
                }
                return op(values)
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

/** Creates a sequential task list with a single task. */
fun aitask(id: String, description: String? = null, op: suspend () -> AiPromptTraceSupport) = AiTaskList(listOf(),
    object: AiTask<Any?, AiPromptTraceSupport>(id, description) {
        override suspend fun execute(input: Any?, context: ExecContext) = op()
    })

/** Initializes [AiTaskList] with a single task that returns an [AiOutput] and has access to the [AiTaskMonitor] for reporting sub-progress. */
fun taskwithmonitor(id: String, description: String? = null, op: suspend (AiTaskMonitor) -> AiOutput): AiTaskList = AiTaskList(listOf(),
    object : AiTask<Any?, AiOutput>(id, description) {
        override suspend fun execute(input: Any?, context: ExecContext) = op(context.monitor)
    })

/** Creates a sequential task list with a single task that has access to the [AiTaskMonitor] for reporting sub-progress. */
fun aitaskwithmonitor(id: String, description: String? = null, op: suspend (AiTaskMonitor) -> AiPromptTraceSupport) = AiTaskList(listOf(),
    object : AiTask<Any?, AiPromptTraceSupport>(id, description) {
        override suspend fun execute(input: Any?, context: ExecContext) = op(context.monitor)
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
 * @throws IllegalArgumentException if there are duplicate task IDs
 */
fun List<AiTask<*, *>>.aggregatetrace(): AiTaskList {
    require(map { it.id }.toSet().size == size) { "Duplicate task IDs" }
    val finalTask = object : AiTask<Any?, List<*>>("promptBatch", dependencies = map { it.id }.toSet()) {
        override suspend fun execute(input: Any?, context: ExecContext): List<*> =
            context.taskInputs.values.toList()
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

