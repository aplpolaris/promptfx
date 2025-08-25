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
package tri.ai.pips.api

import com.fasterxml.jackson.databind.JsonNode

/** An executable unit in a Pips pipeline. */
interface PExecutable {
    /** Tool name. */
    val name: String
    /** Tool version. */
    val version: String
    /** Schema for input. */
    val inputSchema: JsonNode?
    /** Schema for output. */
    val outputSchema: JsonNode?

    /** Execute the tool with given input and context. */
    suspend fun execute(input: JsonNode, context: PExecContext): JsonNode
}

/** Runtime context available to every executable. */
data class PExecContext(
    val vars: MutableMap<String, JsonNode> = mutableMapOf(),
    val resources: Map<String, Any?> = emptyMap(),
    val traceId: String = java.util.UUID.randomUUID().toString()
)
