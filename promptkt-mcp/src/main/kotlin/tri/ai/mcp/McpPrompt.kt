package tri.ai.mcp

import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.serialization.Serializable
import tri.ai.prompt.PromptDef

/**
 * An MCP specification prompt.
 * @see https://modelcontextprotocol.io/specification/2025-06-18/server/prompts
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Serializable
data class McpPrompt(
    val name: String,
    val title: String? = null,
    val description: String? = null,
    val arguments: List<McpPromptArg>? = null
)

/** An argument for an MCP prompt. */
@Serializable
data class McpPromptArg(
    val name: String,
    val description: String,
    val required: Boolean
)

/** Converts a prompt to an MCP contract prompt. */
fun PromptDef.toMcpContract() = McpPrompt(
    name = bareId,
    title = title ?: name ?: bareId.substringAfter('/'),
    description = description,
    arguments = args.map { arg ->
        McpPromptArg(
            name = arg.name,
            description = arg.description ?: "No description provided",
            required = arg.required
        )
    }
)