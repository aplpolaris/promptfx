package tri.ai.mcp.tool

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import tri.ai.mcp.JsonSerializers.toJsonElement
import tri.ai.mcp.JsonSerializers.toJsonNode
import tri.ai.pips.core.ExecContext
import tri.ai.pips.core.Executable
import tri.ai.pips.core.MAPPER
import tri.ai.tool.WebSearchExecutable
import kotlin.String

interface ToolLibrary {
    suspend fun listTools(): List<Executable>
    suspend fun callTool(name: String, args: Map<String, String>): McpToolResult
}

class StarterToolLibrary: ToolLibrary {
    private val tools: List<Executable> = listOf(
        FakeTools.fakeInternetSearch,
        FakeTools.fakeSentimentAnalysis,
        FakeTools.echo,
        WebSearchExecutable()
    )
    override suspend fun listTools(): List<Executable> = tools

    override suspend fun callTool(name: String, args: Map<String, String>): McpToolResult {
        val tool = tools.find { it.name == name }
            ?: return McpToolResult.error(name, "Tool with name '$name' not found")

        val inputNode = MAPPER.valueToTree<JsonNode>(args)
        return try {
            val outputNode = tool.execute(inputNode, ExecContext())
            val outputJsonElement = toJsonElement(outputNode)
            McpToolResult(name, outputJsonElement)
        } catch (e: Exception) {
            McpToolResult.error(name, "Error executing tool: ${e.message}")
        }
    }
}

object FakeTools {
    val echo = object : Executable {
        override val name = "test/echo"
        override val description: String = "An echo tool that returns the input as output."
        override val version: String = "1.0.0"
        override val inputSchema: JsonNode? = MAPPER.readTree(
            """
            {
              "type": "object",
              "properties": {
                "message": { "type": "string", "description": "The message to echo." }
              },
              "required": ["message"]
            }
        """.trimIndent()
        )
        override val outputSchema: JsonNode? = MAPPER.readTree(
            """
            {
              "type": "object",
              "properties": {
                "echoed_message": { "type": "string", "description": "The echoed message." }
              }
            }
        """.trimIndent()
        )
        override suspend fun execute(input: JsonNode, context: ExecContext) = input
    }
    val fakeInternetSearch = StubTool(
        name = "test/internet-search",
        description = "A fake internet search tool that returns a fixed result.",
        hardCodedOutput = buildJsonObject {
            put("query", "example query")
            put("num_results", 3)
            putJsonArray("results") {
                addJsonObject {
                    put("title", "Example Domain")
                    put("url", "https://www.example.com")
                    put("snippet", "This domain is for use in illustrative examples in documents.")
                }
                addJsonObject {
                    put("title", "Example - Wikipedia")
                    put("url", "https://en.wikipedia.org/wiki/Example")
                    put("snippet", "An example is a representative form or pattern.")
                }
                addJsonObject {
                    put("title", "Examples - The Free Dictionary")
                    put("url", "https://www.thefreedictionary.com/examples")
                    put("snippet", "Examples are used to illustrate or explain something.")
                }
            }
        }
    )
    val fakeSentimentAnalysis = StubTool(
        name = "test/sentiment-analysis",
        description = "A fake sentiment analysis tool that returns a fixed sentiment.",
        hardCodedOutput = buildJsonObject {
            put("input_text", "I love programming!")
            put("sentiment", "positive")
            put("confidence", 0.95)
        }
    )
}

/** A tool that returns a fixed result, useful for testing. */
class StubTool(
    override val name: String,
    override val description: String,
    override val version: String = "1.0.0",
    override val inputSchema: JsonNode? = null,
    override val outputSchema: JsonNode? = null,
    @get:JsonIgnore
    val hardCodedOutput: JsonElement
) : Executable {
    override suspend fun execute(input: JsonNode, context: ExecContext) =
        hardCodedOutput.toJsonNode()
}

/** Metadata for a tool/function, following the MCP spec. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Serializable
data class McpToolMetadata(
    val name: String,
    val title: String,
    val description: String,
    val inputSchema: JsonElement?, // Typically a JSON Schema object, or null for no params
    val outputSchema: JsonElement?,
    val summary: String? = null, // Optional: short summary for UI
    val examples: List<JsonElement>? = null // Optional: usage examples
)

/** Result of a tool/function call, following the MCP spec. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Serializable
data class McpToolResult(
    val name: String,
    val output: JsonElement?, // The primary result (could be String, Map, etc.)
    val error: String? = null, // Non-null if tool failed
    val metadata: JsonObject? = null // Optional: extra info (timings, logs, etc.)
) {
    companion object {
        fun error(name: String, error: String) = McpToolResult(name, null, error, null)
    }
}