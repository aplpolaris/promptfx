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
package tri.ai.mcp.tool

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import tri.ai.core.tool.Executable
import tri.ai.mcp.JsonSerializers.toJsonNode

/** Metadata for a tool/function, following the MCP spec. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class McpToolMetadata(
    val name: String,
    val title: String? = null,
    val description: String,
    val inputSchema: JsonNode?, // Typically a JSON Schema object, or null for no params
    val outputSchema: JsonNode?,
    val annotations: Map<String, Any>? = null
)

val McpToolMetadata.version: String?
    get() = this.annotations?.get("version")?.toString()

fun Executable.metadata() = McpToolMetadata(
    name = this.name,
    title = this.name,
    description = this.description,
    inputSchema = this.inputSchema?.let { toJsonNode(it) },
    outputSchema = this.outputSchema?.let { toJsonNode(it) },
    annotations = if (version == "none") null else mapOf("version" to version)
)
