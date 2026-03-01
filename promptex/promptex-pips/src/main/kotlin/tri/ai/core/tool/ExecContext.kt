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
import tri.util.json.jsonMapper
import java.util.UUID

/** Runtime context available to every executable. */
class ExecContext(
    val vars: Map<String, JsonNode> = mutableMapOf(),
    val resources: Map<String, Any?> = emptyMap(),
    val traceId: String = UUID.randomUUID().toString()
) {
    /** Jackson ObjectMapper for JSON operations. */
    val mapper = jsonMapper

    private val vars_mutable = vars as MutableMap<String, JsonNode>

    /** Updates a variable in the context. */
    fun put(key: String, value: JsonNode) {
        vars_mutable[key] = value
        variableSet(key, value)
    }

    /** Hook called when a variable is set. */
    var variableSet: (String, JsonNode) -> Unit = { _, _ -> }

}
