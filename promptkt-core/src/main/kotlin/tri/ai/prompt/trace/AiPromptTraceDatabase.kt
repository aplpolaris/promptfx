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
package tri.ai.prompt.trace

/**
 * In-memory database of prompt traces, allowing for reuse of prompt, model, exec, and output objects,
 * and storage of these in separate tables.
 *
 * TODO - this is not optimized for large databases, but should work well for up to a few thousand
 */
class AiPromptTraceDatabase() {

    var traces = mutableListOf<AiPromptTraceId>()

    var prompts = mutableSetOf<AiPromptInfo>()
    var models = mutableSetOf<AiModelInfo>()
    var execs = mutableSetOf<AiExecInfo>()
    var outputs = mutableSetOf<AiOutputInfo<*>>()

    constructor(traces: Iterable<AiPromptTraceSupport<*>>) : this() {
        addTraces(traces)
    }

    /** Get all prompt traces as list. */
    fun promptTraces() = traces.map { it.promptTrace() }

    /** Get the prompt trace by index. */
    fun AiPromptTraceId.promptTrace() = AiPromptTrace(
        prompts.elementAt(promptIndex),
        models.elementAt(modelIndex),
        execs.elementAt(execIndex),
        outputs.elementAt(outputIndex)
    )

    /** Add all provided traces to the database. */
    fun addTraces(result: Iterable<AiPromptTraceSupport<*>>) {
        result.forEach { addTrace(it) }
    }

    /** Adds the trace to the database, updating object references as needed and returning the object added. */
    fun addTrace(trace: AiPromptTraceSupport<*>): AiPromptTraceId {
        val prompt = addOrGet(prompts, trace.prompt)
        val model = addOrGet(models, trace.model)
        val exec = addOrGet(execs, trace.exec)
        val output = addOrGet(outputs, trace.output)

        return AiPromptTraceId(
            trace.uuid,
            prompts.indexOf(prompt),
            models.indexOf(model),
            execs.indexOf(exec),
            outputs.indexOf(output)
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

