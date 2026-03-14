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
import kotlinx.coroutines.flow.FlowCollector
import tri.ai.pips.AiTaskMonitor
import tri.ai.pips.ExecEvent
import tri.ai.pips.IgnoreMonitor
import tri.ai.prompt.trace.AiPromptTraceSupport
import tri.util.json.jsonMapper
import java.util.UUID

/** Runtime context available to every executable. */
class ExecContext(
    scratchpad: Map<String, JsonNode> = emptyMap(),
    resources: Map<String, Any?> = emptyMap(),
    val traceId: String = UUID.randomUUID().toString(),
    /** Monitor for emitting execution events. */
    val monitor: AiTaskMonitor = IgnoreMonitor,
    /** Previous task outputs (raw values), keyed by task id, for pipeline-style execution. */
    val taskInputs: Map<String, Any?> = emptyMap()
) {
    /** Jackson ObjectMapper for JSON operations. */
    val mapper = jsonMapper

    /** Mutable JSON data store used as a scratchpad for intermediate execution state. */
    val scratchpad: MutableMap<String, JsonNode> = scratchpad.toMutableMap()

    /** Mutable store for runtime service objects (e.g. LLM clients, tool registries) needed during execution. */
    val resources: MutableMap<String, Any?> = resources.toMutableMap()

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

    /** Hook called when a scratchpad entry is set. */
    var variableSet: (String, JsonNode) -> Unit = { _, _ -> }

    /** Returns a copy of this context with the given monitor. */
    fun withMonitor(monitor: FlowCollector<ExecEvent>) =
        ExecContext(scratchpad, resources, traceId, monitor, taskInputs)

    /** Returns a copy of this context with the given task inputs. */
    fun withTaskInputs(taskInputs: Map<String, Any?>) =
        ExecContext(scratchpad, resources, traceId, monitor, taskInputs)

}
