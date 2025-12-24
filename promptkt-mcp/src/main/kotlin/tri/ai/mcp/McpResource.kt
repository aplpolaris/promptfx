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
data class McpReadResourceResponse(
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