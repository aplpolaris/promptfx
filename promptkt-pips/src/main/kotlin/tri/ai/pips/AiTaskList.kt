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
import tri.ai.prompt.trace.AiOutput
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiPromptTraceSupport

/** Stores a list of tasks that can be executed in order. */
class AiTaskList(tasks: List<AiTask>, val lastTask: AiTask) {

    val plan = tasks + lastTask
    val planner
        get() = object : AiPlanner {
            override fun plan() = plan
        }

    constructor(firstTaskId: String, description: String? = null, op: suspend () -> AiPromptTraceSupport): this(listOf(),
        object: AiTask(firstTaskId, description) {
            override suspend fun execute(inputs: Map<String, AiPromptTraceSupport>, monitor: AiTaskMonitor) = op()
        })

    //region BUILDER METHODS

    /**
     * Adds a task to the end of the list, where invoking the task performs the given operation.
     * The input to the task is the first output from the final task in this [AiTaskList]. (Use [taskonlist] to apply to the full output list.)
     * The result will be wrapped in an [AiPromptTrace] object with a single result.
     * @throws IllegalArgumentException if the result is an [AiPromptTraceSupport] object, instead use [aitask]
     */
    fun task(id: String, description: String? = null, op: suspend (AiOutput) -> String) =
        aitask(id, description) {
            val t0 = System.currentTimeMillis()
            val res = op(it)
            AiPromptTrace(execInfo = AiExecInfo.durationSince(t0), outputInfo = AiOutputInfo.text(res))
        }

    /**
     * Adds an AI task to the end of the list, where invoking the task performs the given operation.
     * The input to the task is the first output from the final task in this [AiTaskList]. (Use [aitaskonlist] to apply to the full output list.)
     * The result is expected to be a [AiPromptTraceSupport] object with details about model, execution time, etc.
     */
    fun aitask(id: String, description: String? = null, op: suspend (AiOutput) -> AiPromptTraceSupport): AiTaskList {
        val newTask = object : AiTask(id, description, setOf(lastTask.id)) {
            override suspend fun execute(inputs: Map<String, AiPromptTraceSupport>, monitor: AiTaskMonitor): AiPromptTraceSupport {
                val result = inputs[lastTask.id] as AiPromptTraceSupport
                return op(result.firstValue)
            }
        }
        return AiTaskList(plan, newTask)
    }

    /**
     * Adds a task to the end of the list, where invoking the task performs the given operation.
     * The input to the task is the full list of outputs from the final task in this [AiTaskList].
     */
    fun aitaskonlist(id: String, description: String? = null, op: suspend (List<AiOutput>) -> AiPromptTraceSupport): AiTaskList {
        val newTask = object : AiTask(id, description, setOf(lastTask.id)) {
            override suspend fun execute(inputs: Map<String, AiPromptTraceSupport>, monitor: AiTaskMonitor): AiPromptTraceSupport {
                val result = inputs[lastTask.id] as AiPromptTraceSupport
                return op(result.values ?: listOf())
            }
        }
        return AiTaskList(plan, newTask)
    }

    //endregion

}

//region BUILDER METHODS

/**
 * Creates a sequential task list using provided tasks.
 * @throws NoSuchElementException if the list is empty
 */
fun <T> aitasklist(tasks: List<AiTask>) =
    AiTaskList(tasks.dropLast(1), tasks.last())

/** Creates a sequential task list with a single task. */
fun <T : Any> task(id: String, description: String? = null, op: suspend () -> T): AiTaskList =
    aitask(id, description) {
        val t0 = System.currentTimeMillis()
        val res = op()
        if (res is AiPromptTraceSupport) throw IllegalArgumentException("Use aitask() for AiTaskResult")
        AiPromptTrace(execInfo = AiExecInfo.durationSince(t0), outputInfo = AiOutputInfo.other(res, allowList = true))
    }

/** Creates a sequential task list with a single task. */
fun aitask(id: String, description: String? = null, op: suspend () -> AiPromptTraceSupport): AiTaskList =
    AiTaskList(id, description, op)

/**
 * Create a [AiTaskList] for a list of tasks that all return the same type, where the last task returns the list of results from individual tasks.
 * @throws IllegalArgumentException if there are duplicate task IDs
 */
inline fun List<AiTask>.aggregate(): AiTaskList {
    require(map { it.id }.toSet().size == size) { "Duplicate task IDs" }
    val finalTask = object : AiTask("promptBatch", dependencies = map { it.id }.toSet()) {
        override suspend fun execute(inputs: Map<String, AiPromptTraceSupport>, monitor: AiTaskMonitor): AiPromptTrace {
            val aggregateResults = inputs.values.map {
                it.output!!.outputs.map { it.content() }
            }
            // TODO - aggregate other parts of the intermediate results, e.g. model ids, prompts, etc.
            return AiPromptTrace(outputInfo = AiOutputInfo.output(AiOutput(other = aggregateResults)))
        }
    }
    return AiTaskList(this, finalTask)
}

/**
 * Create a [AiTaskList] for a list of tasks that all return the same type, where the last task returns the list of results from individual tasks.
 * @throws IllegalArgumentException if there are duplicate task IDs
 */
fun List<AiTask>.aggregatetrace(): AiTaskList {
    require(map { it.id }.toSet().size == size) { "Duplicate task IDs" }
    val finalTask = object : AiTask("promptBatch", dependencies = map { it.id }.toSet()) {
        override suspend fun execute(inputs: Map<String, AiPromptTraceSupport>, monitor: AiTaskMonitor): AiPromptTrace {
            val aggregateResults = inputs.values.toList()
            return AiPromptTrace(outputInfo = AiOutputInfo.output(AiOutput(other = aggregateResults)))
        }
    }
    return AiTaskList(this, finalTask)
}

//endregion
