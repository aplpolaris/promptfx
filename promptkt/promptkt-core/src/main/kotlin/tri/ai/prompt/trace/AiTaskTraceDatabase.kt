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
package tri.ai.prompt.trace

/**
 * In-memory database of [AiTaskTrace] objects, enabling deduplication of model, exec, and output
 * components for memory-efficient storage.
 *
 * Designed as the replacement for the legacy [AiPromptTraceDatabase]. Use this class for all new code.
 *
 * TODO - this is not optimized for large databases, but should work well for up to a few thousand traces.
 */
class AiTaskTraceDatabase {

    var traces = mutableListOf<AiTaskTraceId>()

    var envs = mutableSetOf<AiEnvInfo>()
    var inputs = mutableSetOf<AiTaskInputInfo>()
    var execs = mutableSetOf<AiExecInfo>()
    var outputs = mutableSetOf<AiOutputInfo>()

    constructor()

    constructor(traces: Iterable<AiTaskTrace>) {
        addTraces(traces)
    }

    /** Get all traces as a list. */
    fun taskTraces() = traces.map { it.taskTrace() }

    /** Reconstruct the full [AiTaskTrace] from an [AiTaskTraceId]. */
    fun AiTaskTraceId.taskTrace() = AiTaskTrace(
        taskId = taskId,
        parentTaskId = parentTaskId,
        callerId = callerId,
        env = envIndex?.let { envs.elementAt(it) },
        input = inputIndex?.let { inputs.elementAt(it) },
        exec = execs.elementAt(execIndex),
        output = outputIndex?.let { outputs.elementAt(it) }
    )

    /** Add all provided traces to the database. */
    fun addTraces(result: Iterable<AiTaskTrace>) {
        result.forEach { addTrace(it) }
    }

    /** Adds the trace to the database, deduplicating components, and returns the stored ID record. */
    fun addTrace(trace: AiTaskTrace): AiTaskTraceId {
        val env = addOrGet(envs, trace.env)
        val input = addOrGet(inputs, trace.input)
        val exec = addOrGet(execs, trace.exec)
        val output = addOrGet(outputs, trace.output)

        return AiTaskTraceId(
            taskId = trace.taskId,
            parentTaskId = trace.parentTaskId,
            callerId = trace.callerId,
            envIndex = env?.let { envs.indexOf(it) },
            inputIndex = input?.let { inputs.indexOf(it) },
            execIndex = execs.indexOf(exec),
            outputIndex = output?.let { outputs.indexOf(it) }
        ).also {
            traces.add(it)
        }
    }

    private fun <T> addOrGet(set: MutableSet<T>, item: T?): T? {
        val existing = set.find { it == item }
        return if (existing != null)
            existing
        else {
            if (item != null)
                set.add(item)
            item
        }
    }

}

/**
 * Database reference for a stored [AiTaskTrace], using component indices to avoid duplication.
 */
data class AiTaskTraceId(
    val taskId: String,
    val parentTaskId: String? = null,
    val callerId: String? = null,
    val envIndex: Int? = null,
    val inputIndex: Int? = null,
    val execIndex: Int,
    val outputIndex: Int? = null
)
