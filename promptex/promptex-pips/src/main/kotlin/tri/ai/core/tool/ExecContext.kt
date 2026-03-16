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
package tri.ai.core.tool

import com.fasterxml.jackson.databind.JsonNode
import tri.ai.pips.AiTaskMonitor
import tri.ai.pips.IgnoreMonitor
import tri.ai.prompt.trace.AiPromptTraceSupport
import tri.util.json.jsonMapper
import java.util.*

/** Runtime context available to every executable. */
class ExecContext(
    /** A unique identifier for this execution, used for tracing and logging. */
    val traceId: String = UUID.randomUUID().toString(),
    /** Mutable store for runtime service objects (e.g. LLM clients, tool registries) needed during execution. */
    val resources: MutableMap<String, Any?> = mutableMapOf(),
    /** Previous task outputs (raw values), keyed by task id, for pipeline-style execution. */
    val taskOutputs: MutableMap<String, Any?> = mutableMapOf(),
    /** Mutable JSON data store used as a scratchpad for intermediate execution state. */
    val scratchpad: MutableMap<String, JsonNode> = mutableMapOf(),
    /** Monitor for emitting execution events. */
    val monitor: AiTaskMonitor = IgnoreMonitor
) {
    /** Jackson ObjectMapper for JSON operations. */
    val mapper = jsonMapper

    /** Log of traces emitted by tasks during execution, keyed by task id. */
    val traces: MutableMap<String, AiPromptTraceSupport> = mutableMapOf()

    /** Updates a scratchpad entry in the context. */
    fun put(key: String, value: JsonNode) {
        scratchpad[key] = value
        variableSet(key, value)
    }

    /** Stores a named resource object in the context. */
    fun putResource(key: String, value: Any?) {
        resources[key] = value
    }

    /** Logs a trace for the given task id. */
    fun logTrace(id: String, trace: AiPromptTraceSupport) {
        traces[id] = trace
    }

    /** Chronological log of execution steps taken during this run, written by all executor types. */
    val steps: MutableList<ExecStep> = mutableListOf()

    /** Appends a step to the execution log. */
    fun logStep(step: ExecStep) { steps.add(step) }

    /** Records a tool or solver invocation in the execution log. */
    fun logToolCall(toolName: String, input: String, output: String, durationMs: Long) =
        logStep(ExecStep.ToolCall(toolName, input, output, durationMs))

    /** Records a reasoning or thought step in the execution log. */
    fun logReasoning(content: String) = logStep(ExecStep.Reasoning(content))

    /** Records a planning or decomposition update in the execution log. */
    fun logPlanUpdate(description: String) = logStep(ExecStep.PlanUpdate(description))

    /** Records the completion or failure of a named task in the execution log. */
    fun logTaskResult(taskId: String, succeeded: Boolean, durationMs: Long) =
        logStep(ExecStep.TaskResult(taskId, succeeded, durationMs))

    /** Hook called when a scratchpad entry is set. */
    var variableSet: (String, JsonNode) -> Unit = { _, _ -> }

}
