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

/**
 * Stores a collection of tasks with dependencies, with builders making it easy to chain tasks together.
 * The final task in the list is the one that returns the value of type [T], and all other tasks are assumed to be dependencies that produce intermediate outputs.
 */
class AiTaskBuilder<T: Any>(tasks: List<AiTask<*, *>>, val lastTask: AiTask<*, T>) {

    /** List of tasks to be executed. */
    val plan = tasks + lastTask

    companion object {
        /** Initializes [AiTaskBuilder] with a single task that returns a value of type [T]. */
        fun <T : Any> task(id: String, description: String? = null, op: suspend (ExecContext) -> T) =
            AiTaskBuilder(listOf(), AiTask.task(id, description, op))
    }

}

//region BUILDER METHODS

/**
 * Adds a task to the end of the list, where the predecessor's output is cast to [S] and the result [T] is returned directly.
 * The input to the task is the first output from the final task in this [AiTaskBuilder].
 */
inline fun <reified S: Any, reified T : Any> AiTaskBuilder<S>.task(id: String, description: String? = null, crossinline op: suspend (S, ExecContext) -> T): AiTaskBuilder<T> {
    val newTask = object : AiTask<S, T>(id, description, setOf(lastTask.id)) {
        override suspend fun execute(input: S, context: ExecContext) = op(input, context)
    }
    return AiTaskBuilder(plan, newTask)
}

/**
 * Adds a task on the end of a list, where the final task returns an iterable, that transforms each item of that iterable
 * by applying the provided operation to it, and returns a list of results.
 */
inline fun <reified S, reified L : Iterable<S>, reified T : Any> AiTaskBuilder<L>.taskOnEach(id: String, description: String? = null, crossinline op: suspend (S, ExecContext) -> T): AiTaskBuilder<List<T>> {
    val newTask = object : AiTask<L, List<T>>(id, description, setOf(lastTask.id)) {
        override suspend fun execute(input: L, context: ExecContext) = input.map { op(it, context) }
    }
    return AiTaskBuilder(plan, newTask)
}

/**
 * Create a [AiTaskBuilder] for a list of tasks that all return the same type, where the last task returns the list of results from individual tasks.
 * @throws ClassCastException if one of the individual task outputs cannot be cast to [T]
 * @throws IllegalArgumentException if there are duplicate task IDs
 */
inline fun <reified T> List<AiTask<*, T>>.aggregate(): AiTaskBuilder<List<T>> {
    require(map { it.id }.toSet().size == size) { "Duplicate task IDs" }
    val finalTask = object : AiTask<Any?, List<T>>("promptBatch", dependencies = map { it.id }.toSet()) {
        override suspend fun execute(input: Any?, context: ExecContext): List<T> {
            val priorOutputs = context.taskOutputs.filterKeys { it in dependencies }
            return priorOutputs.map { it.value as T }.toList()
        }
    }
    return AiTaskBuilder(this, finalTask)
}

//endregion

