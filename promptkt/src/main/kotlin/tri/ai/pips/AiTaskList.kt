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

import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiPromptTraceSupport

/** Stores a list of tasks that can be executed in order. */
class AiTaskList<S>(tasks: List<AiTask<*>>, val lastTask: AiTask<S>) {

    val plan = tasks + lastTask
    val planner
        get() = object : AiPlanner {
            override fun plan() = plan
        }

    constructor(firstTaskId: String, description: String? = null, op: suspend () -> AiPromptTraceSupport<S>): this(listOf(),
        object: AiTask<S>(firstTaskId, description) {
            override suspend fun execute(inputs: Map<String, AiPromptTraceSupport<*>>, monitor: AiTaskMonitor) = op()
        })

    /** Adds a task to the end of the list. */
    fun <T> task(id: String, description: String? = null, op: suspend (S) -> T) =
        aitask(id, description) {
            val t0 = System.currentTimeMillis()
            val res = op(it)
            if (res is AiPromptTraceSupport<*>) throw IllegalArgumentException("Use aitask() for AiPromptTraceSupport")
            AiPromptTrace(execInfo = AiExecInfo.durationSince(t0), outputInfo = AiOutputInfo(listOf(res)))
        }

    /** Adds a task to the end of the list. */
    fun <T> aitask(id: String, description: String? = null, op: suspend (S) -> AiPromptTraceSupport<T>): AiTaskList<T> {
        val newTask = object : AiTask<T>(id, description, setOf(lastTask.id)) {
            override suspend fun execute(inputs: Map<String, AiPromptTraceSupport<*>>, monitor: AiTaskMonitor): AiPromptTraceSupport<T> {
                // TODO - eventually, add support for multiple outputs from previous task
                val result = inputs[lastTask.id] as AiPromptTraceSupport<S>
                return op(result.firstValue)
            }
        }
        return AiTaskList(plan, newTask)
    }

    /** Adds a task to the end of the list. */
    fun <T> aitasklist(id: String, description: String? = null, op: suspend (List<S>) -> AiPromptTraceSupport<T>): AiTaskList<T> {
        val newTask = object : AiTask<T>(id, description, setOf(lastTask.id)) {
            override suspend fun execute(inputs: Map<String, AiPromptTraceSupport<*>>, monitor: AiTaskMonitor): AiPromptTraceSupport<T> {
                val result = inputs[lastTask.id] as AiPromptTraceSupport<S>
                return op(result.values ?: listOf())
            }
        }
        return AiTaskList(plan, newTask)
    }

    /** Adds a task to the end of the list. */
    fun <T> aiprompttask(id: String, description: String? = null, op: suspend (AiPromptTraceSupport<S>) -> AiPromptTraceSupport<T>): AiTaskList<T> {
        val newTask = object : AiTask<T>(id, description, setOf(lastTask.id)) {
            override suspend fun execute(inputs: Map<String, AiPromptTraceSupport<*>>, monitor: AiTaskMonitor): AiPromptTraceSupport<T> {
                // TODO - eventually, add support for multiple outputs from previous task
                val result = inputs[lastTask.id] as AiPromptTraceSupport<S>
                return op(result)
            }
        }
        return AiTaskList(plan, newTask)
    }

}

//region BUILDER METHODS

/**
 * Create a [AiTaskList] for a list of tasks that all return the same type, where the last task returns the list of results from individual tasks.
 * @throws IllegalArgumentException if there are duplicate task IDs
 */
inline fun <reified T> List<AiTask<T>>.aggregate(): AiTaskList<List<T>> {
    require(map { it.id }.toSet().size == size) { "Duplicate task IDs" }
    val finalTask = object : AiTask<List<T>>("promptBatch", dependencies = map { it.id }.toSet()) {
        override suspend fun execute(inputs: Map<String, AiPromptTraceSupport<*>>, monitor: AiTaskMonitor): AiPromptTrace<List<T>> {
            val aggregateResults = inputs.values.map {
                (it.output?.outputs ?: listOf<T>()) as List<T>
            }
            // TODO - aggregate other parts of the intermediate results, e.g. model ids, prompts, etc.
            return AiPromptTrace(outputInfo = AiOutputInfo(aggregateResults))
        }
    }
    return AiTaskList(this, finalTask)
}

/**
 * Create a [AiTaskList] for a list of tasks that all return the same type, where the last task returns the list of results from individual tasks.
 * @throws IllegalArgumentException if there are duplicate task IDs
 */
inline fun <reified T> List<AiTask<T>>.aggregatetrace(): AiTaskList<AiPromptTraceSupport<T>> {
    require(map { it.id }.toSet().size == size) { "Duplicate task IDs" }
    val finalTask = object : AiTask<AiPromptTraceSupport<T>>("promptBatch", dependencies = map { it.id }.toSet()) {
        override suspend fun execute(inputs: Map<String, AiPromptTraceSupport<*>>, monitor: AiTaskMonitor): AiPromptTrace<AiPromptTraceSupport<T>> {
            val aggregateResults = inputs.values.toList() as List<AiPromptTraceSupport<T>>
            return AiPromptTrace(outputInfo = AiOutputInfo(aggregateResults))
        }
    }
    return AiTaskList(this, finalTask)
}

/**
 * Creates a sequential task list using provided tasks.
 * @throws NoSuchElementException if the list is empty
 */
fun <T> aitasklist(tasks: List<AiTask<T>>) =
    AiTaskList(tasks.dropLast(1), tasks.last())

/** Creates a sequential task list with a single task. */
fun <T> task(id: String, description: String? = null, op: suspend () -> T): AiTaskList<T> =
    aitask(id, description) {
        val t0 = System.currentTimeMillis()
        val res = op()
        if (res is AiPromptTraceSupport<*>) throw IllegalArgumentException("Use aitask() for AiTaskResult")
        AiPromptTrace(execInfo = AiExecInfo.durationSince(t0), outputInfo = AiOutputInfo(listOf(res)))
    }

/** Creates a sequential task list with a single task. */
fun <T> aitask(id: String, description: String? = null, op: suspend () -> AiPromptTraceSupport<T>): AiTaskList<T> =
    AiTaskList(id, description, op)

//endregion
