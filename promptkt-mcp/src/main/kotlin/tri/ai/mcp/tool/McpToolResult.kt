package tri.ai.mcp.tool

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.JsonNode
import tri.ai.mcp.McpResource

/** Result of a tool/function call, following the MCP spec. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class McpToolResult(
    val content: List<McpContent>,
    val structuredContent: JsonNode? = null, // Optional: structured result
    val isError: Boolean? = null, // Non-null if tool failed
    val metadata: JsonNode? = null // Optional: extra info (timings, logs, etc.)
) {

    /** Gets an error message, if this result represents an error. */
    fun errorMessage(): String? {
        if (isError != true) return null
        val textContents = content.filterIsInstance<McpContent.Text>()
        return if (textContents.isNotEmpty()) {
            textContents.joinToString("\n") { it.text }
        } else {
            "Unknown error"
        }
    }

    companion object {
        /** Errors are wrapped as text content. */
        fun error(error: String) = McpToolResult(
            content = listOf(McpContent.Text(error)),
            isError = true
        )

        /** Convert structured output to McpToolResult. */
        fun createStructured(node: JsonNode) = McpToolResult(
            content = listOf(McpContent.Text(node.toString())),
            structuredContent = node,
        )
    }
}

/** Content item in a tool result, can be text, image, audio, resource links, or embedded resources. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = McpContent.Text::class, name = "text"),
    JsonSubTypes.Type(value = McpContent.Image::class, name = "image"),
    JsonSubTypes.Type(value = McpContent.Audio::class, name = "audio"),
    JsonSubTypes.Type(value = McpContent.ResourceLink::class, name = "resource_link"),
    JsonSubTypes.Type(value = McpContent.Resource::class, name = "resource")
)
sealed class McpContent(val type: String) {
    val annotations: Map<String, JsonNode>? = null
    data class Text(val text: String) : McpContent("text")
    data class Image(val data: String, val mimeType: String) : McpContent("image")
    data class Audio(val data: String, val mimeType: String) : McpContent("audio")
    data class ResourceLink(private val r: McpResource) : McpContent("resource_link") {
        val uri: String by r::uri
        val name: String by r::name
        val title: String? by r::title
        val description: String? by r::description
        val mimeType: String? by r::mimeType
    }
    data class Resource(val uri: String, val mimeType: String?, val text: String) : McpContent("resource")
}