package tri.ai.mcp.tool

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.serialization.json.JsonPrimitive
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