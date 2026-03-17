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
    /** Monitor for emitting execution events. */
    val monitor: AiTaskMonitor = IgnoreMonitor
) {
    /** Jackson ObjectMapper for JSON operations. */
    val mapper = jsonMapper

    private val _resources: MutableMap<String, Any?> = mutableMapOf()
    private val _scratchpad: MutableMap<String, Any?> = mutableMapOf()
    private val _traces: MutableMap<String, AiPromptTraceSupport> = mutableMapOf()

    /** Hook called whenever a scratchpad entry is set via [put]. */
    var variableSet: (String, Any?) -> Unit = { _, _ -> }

    /** Read-only view of traces emitted by tasks during execution, keyed by task id. */
    val traces: Map<String, AiPromptTraceSupport>
        get() = _traces

    //region RESOURCES

    /** Stores a named resource object in the context. */
    fun putResource(key: String, value: Any?) {
        _resources[key] = value
    }

    /** Returns the resource for [key], or null if absent. */
    fun resource(key: String): Any? = _resources[key]

    //endregion

    //region SCRATCHPAD

    /** Stores an entry in the scratchpad and fires the [variableSet] hook. */
    fun put(key: String, value: Any?) {
        _scratchpad[key] = value
        variableSet(key, value)
    }

    /** Returns the scratchpad entry for [key], or null if absent. */
    fun get(key: String): Any? = _scratchpad[key]

    /** Returns the scratchpad entry for [key] as a [JsonNode], or null if absent or not a [JsonNode]. */
    fun getJson(key: String): JsonNode? = _scratchpad[key] as? JsonNode

    /** Returns a read-only view of all [JsonNode] entries in the scratchpad, filtering out non-JSON values. */
    fun jsonScratchpad(): Map<String, JsonNode> = _scratchpad.entries
        .mapNotNull { (k, v) -> (v as? JsonNode)?.let { k to it } }
        .toMap()

    //endregion

    //region TRACES

    /** Logs a trace for the given task id. */
    fun logTrace(id: String, trace: AiPromptTraceSupport) {
        _traces[id] = trace
    }

    /** Returns the trace for [id], or null if absent. */
    fun trace(id: String): AiPromptTraceSupport? = _traces[id]

    //endregion

    //region FACTORY

    /** Creates a child context that inherits resources and monitor  from this context. */
    fun childContext(): ExecContext = ExecContext(monitor = monitor).also { child ->
        _resources.forEach { (k, v) -> child.putResource(k, v) }
    }

    //endregion

}
