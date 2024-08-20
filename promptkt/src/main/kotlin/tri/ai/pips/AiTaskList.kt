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

/** Stores a list of tasks that can be executed in order. */
class AiTaskList<S>(tasks: List<AiTask<*>>, val lastTask: AiTask<S>) {

    val plan = tasks + lastTask
    val planner
        get() = object : AiPlanner {
            override fun plan() = plan
        }

    constructor(firstTaskId: String, description: String? = null, op: suspend () -> AiTaskResult<S>): this(listOf(),
        object: AiTask<S>(firstTaskId, description) {
            override suspend fun execute(inputs: Map<String, AiTaskResult<*>>, monitor: AiTaskMonitor) = op()
        })

    /** Adds a task to the end of the list. */
    fun <T> task(id: String, description: String? = null, op: suspend (S) -> T) =
        aitask(id, description) {
            val res = op(it)
            if (res is AiTaskResult<*>) throw IllegalArgumentException("Use aitask() for AiTaskResult")
            AiTaskResult.result(res, modelId = null)
        }

    /** Adds a task to the end of the list. */
    fun <T> aitask(id: String, description: String? = null, op: suspend (S) -> AiTaskResult<T>): AiTaskList<T> {
        val newTask = object : AiTask<T>(id, description, setOf(lastTask.id)) {
            override suspend fun execute(inputs: Map<String, AiTaskResult<*>>, monitor: AiTaskMonitor) =
                op(inputs[lastTask.id]!!.values as S)
        }
        return AiTaskList(plan, newTask)
    }

}

//region BUILDER METHODS

/**
 * Create a [AiTaskList] for a list of tasks that all return the same type, where the last task returns the list of results from individual tasks.
 * @throws IllegalArgumentException if there are duplicate task IDs
 */
inline fun <reified T> List<AiTask<T>>.aggregate(): AiTaskList<T> {
    require(map { it.id }.toSet().size == size) { "Duplicate task IDs" }
    val finalTask = object : AiTask<T>("promptBatch", dependencies = map { it.id }.toSet()) {
        override suspend fun execute(inputs: Map<String, AiTaskResult<*>>, monitor: AiTaskMonitor) =
            AiTaskResult.results((inputs.values as Collection<AiTaskResult<T>>).flatMap { it.values ?: listOf() }.toList())
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
        val res = op()
        if (res is AiTaskResult<*>) throw IllegalArgumentException("Use aitask() for AiTaskResult")
        AiTaskResult.result(res, modelId = null)
    }

/** Creates a sequential task list with a single task. */
fun <T> aitask(id: String, description: String? = null, op: suspend () -> AiTaskResult<T>): AiTaskList<T> =
    AiTaskList(id, description, op)

//endregion
