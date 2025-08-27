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
//        FakeTools.fakeInternetSearch,
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
    private fun buildSchemaWithOneRequiredParam(paramName: String, paramDescription: String) = MAPPER.readTree(
        """
        {
          "type": "object",
          "properties": {
            "$paramName": { "type": "string", "description": "$paramDescription" }
          },
          "required": ["$paramName"]
        }
    """.trimIndent()
    )
    private fun buildSchemaWithOneOptionalParam(paramName: String, paramDescription: String) = MAPPER.readTree(
        """
        {
          "type": "object",
          "properties": {
            "$paramName": { "type": "string", "description": "$paramDescription" }
          }
        }
    """.trimIndent()
    )

    val echo = object : Executable {
        override val name = "test_echo"
        override val description: String = "An echo tool that returns the input as output."
        override val version: String = "1.0.0"
        override val inputSchema: JsonNode? =
            buildSchemaWithOneRequiredParam("message", "The message to echo.")
        override val outputSchema: JsonNode? =
            buildSchemaWithOneOptionalParam("echoed_message", "The echoed message.")
        override suspend fun execute(input: JsonNode, context: ExecContext) = input
    }
    val fakeInternetSearch = StubTool(
        name = "test_internet_search",
        description = "A fake internet search tool that returns a fixed result.",
        inputSchema = buildSchemaWithOneRequiredParam("query", "The search query."),
        outputSchema = MAPPER.readTree("""
            {
              "type": "object",
              "properties": {
                "query": { "type": "string", "description": "The search query." },
                "num_results": { "type": "integer", "description": "Number of results returned." },
                "results": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "title": { "type": "string", "description": "Title of the result." },
                      "url": { "type": "string", "description": "URL of the result." },
                      "snippet": { "type": "string", "description": "Short description or snippet." }
                    },
                    "required": ["title", "url"]
                  },
                  "description": "List of search results."
                }
              },
              "required": ["results"]
            }
        """.trimIndent()),
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
        name = "test_sentiment_analysis",
        description = "A fake sentiment analysis tool that returns a fixed sentiment.",
        inputSchema = buildSchemaWithOneRequiredParam("input_text", "The text to analyze."),
        outputSchema = MAPPER.readTree("""
            {
              "type": "object",
              "properties": {
                "input_text": { "type": "string", "description": "The input text that was analyzed." },
                "sentiment": { 
                  "type": "string", 
                  "description": "The detected sentiment.", 
                  "enum": ["positive", "negative", "neutral"] 
                },
                "confidence": { 
                  "type": "number", 
                  "description": "Confidence score between 0 and 1." ,
                  "minimum": 0,
                  "maximum": 1
                }
              },
              "required": ["input_text", "sentiment", "confidence"]
            }
        """.trimIndent()),
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
    override val inputSchema: JsonNode, // required for Claude Desktop to work
    override val outputSchema: JsonNode, // required for Claude Desktop to work
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
