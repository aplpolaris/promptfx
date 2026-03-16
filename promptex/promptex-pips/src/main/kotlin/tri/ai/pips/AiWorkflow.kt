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
import tri.ai.prompt.trace.AiPromptTrace

/**
 * A workflow that encapsulates a list of tasks as a single composable [AiTask].
 * When executed, all internal [tasks] are run via [AiPipelineExecutor] within a child [ExecContext]
 * that inherits [ExecContext.resources] and [ExecContext.monitor] from the parent context.
 * The parent [input] (if non-null) is stored under this workflow's [id] in the child context's
 * [ExecContext.scratchpad], allowing inner tasks to reference it by declaring this workflow's [id]
 * as a dependency.
 * After execution, each inner task trace is merged into the parent [ExecContext.traces] under the
 * key `"<workflowId>/<taskId>"`.
 * If the inner pipeline fails, an [IllegalStateException] is thrown so the outer pipeline marks
 * this workflow task as failed.
 *
 * Use [AiTaskBuilder.asWorkflow] to create an [AiWorkflow] from an [AiTaskBuilder].
 */
class AiWorkflow<in I, out O>(
    id: String,
    description: String? = null,
    dependencies: Set<String> = setOf(),
    /** Ordered list of tasks to execute as part of this workflow. */
    val tasks: List<AiTask<*, *>>,
) : AiTask<I, O>(id, description, dependencies) {

    init {
        require(tasks.isNotEmpty()) { "AiWorkflow '$id' must have at least one task." }
    }

    override suspend fun execute(input: I, context: ExecContext): O {
        // Create a child context that inherits resources and monitoring but has isolated task state.
        val innerContext = context.childContext()
        // Make the outer input available so inner tasks may depend on this workflow's id.
        if (input != null) {
            innerContext.put(id, input)
            // Add a synthetic successful trace so inner tasks declaring this workflow's id as a
            // dependency can be scheduled (AiPipelineExecutor checks traces for dependency readiness).
            innerContext.logTrace(id, AiPromptTrace())
        }

        val result = AiPipelineExecutor.execute(tasks, innerContext)

        // Merge inner task traces into the outer context, prefixed with this workflow's id.
        innerContext.traces.forEach { (taskId, trace) ->
            context.logTrace("$id/$taskId", trace)
        }

        // Propagate inner pipeline failure as an exception so the outer pipeline marks this task failed.
        if (!result.finalResult.exec.succeeded()) {
            throw IllegalStateException(
                "Workflow '$id' failed: ${result.finalResult.errorMessage ?: "unknown error"}"
            )
        }

        @Suppress("UNCHECKED_CAST")
        return innerContext.scratchpad[tasks.last().id] as O
    }
}

/**
 * Wraps this builder's entire task plan as a single composable [AiWorkflow] task.
 * The workflow's typed output [T] is the output of this builder's last task.
 */
fun <T : Any> AiTaskBuilder<T>.asWorkflow(
    id: String,
    description: String? = null,
    dependencies: Set<String> = setOf()
): AiWorkflow<Any?, T> = AiWorkflow(id, description, dependencies, plan)
