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
package tri.ai.mcp

import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * An MCP specification resource.
 * @see https://modelcontextprotocol.io/specification/2025-06-18/server/resources
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Serializable
data class McpResource(
    val uri: String,
    val name: String,
    val title: String? = null,
    val description: String? = null,
    val mimeType: String? = null
) {
    val annotations: Map<String, JsonElement>? = null
}

/**
 * An MCP resource template - a template for a resource URI with arguments.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Serializable
data class McpResourceTemplate(
    val uriTemplate: String,
    val name: String,
    val title: String? = null,
    val description: String? = null,
    val mimeType: String? = null
)

/**
 * Response returned from a resource read request.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Serializable
data class McpResourceResponse(
    val contents: List<McpResourceContents>
)

/**
 * Contents of a resource - either text or binary.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Serializable
data class McpResourceContents(
    val uri: String,
    val mimeType: String? = null,
    val text: String? = null,
    val blob: String? = null
)
