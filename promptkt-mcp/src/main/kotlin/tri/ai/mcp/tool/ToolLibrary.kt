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
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.Executable
import tri.ai.core.tool.impl.WebSearchExecutable
import tri.ai.mcp.JsonSerializers.toJsonNode
import tri.util.json.jsonMapper
import kotlin.String

interface ToolLibrary {
    suspend fun listTools(): List<McpToolMetadata>
    suspend fun getTool(name: String): McpToolMetadata?
    suspend fun callTool(name: String, args: Map<String, Any?>): McpToolResult
}

class StarterToolLibrary: ToolLibrary {
    var tools: List<Executable> = listOf(WebSearchExecutable()) + FakeTools.load()
    override suspend fun listTools(): List<McpToolMetadata> = tools.map { it.metadata() }

    override suspend fun getTool(name: String): McpToolMetadata? =
        tools.find { it.name == name }?.metadata()

    override suspend fun callTool(name: String, args: Map<String, Any?>): McpToolResult {
        val tool = tools.find { it.name == name }
            ?: return McpToolResult.error("Tool with name '$name' not found")

        val inputNode = jsonMapper.valueToTree<JsonNode>(args)
        return try {
            val outputNode = tool.execute(inputNode, ExecContext())
            McpToolResult(outputNode)
        } catch (e: Exception) {
            McpToolResult.error("Error executing tool: ${e.message}")
        }
    }
}

object FakeTools {
    // Remove local buildSchemaWithOneRequiredParam - now using common utility from tri.util.json
    // Remove local buildSchemaWithOneOptionalParam - now using common utility from tri.util.json

    fun load(): List<StubTool> {
        val resource = this::class.java.getResource("resources/stub-tools.json")!!
        val loadedTools = jsonMapper.readValue<Map<String, List<StubTool>>>(resource)
        return loadedTools.values.flatten()
//        listOf(fakeInternetSearch, fakeSentimentAnalysis, echo, testAircraftTypeLookup, testAviationNewsSearch, testAircraftTracksSearch, testAviationPersonLookup)
    }
}

/** Metadata for a tool/function, following the MCP spec. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class McpToolMetadata(
    val name: String,
    val title: String? = null,
    val description: String,
    val inputSchema: JsonNode?, // Typically a JSON Schema object, or null for no params
    val outputSchema: JsonNode?,
    val annotations: Map<String, JsonNode>? = null // Optional: extra metadata
)

val McpToolMetadata.version: String?
    get() = this.annotations?.get("version")?.asText()

fun Executable.metadata() = McpToolMetadata(
    name = this.name,
    title = this.name,
    description = this.description,
    inputSchema = this.inputSchema?.let { toJsonNode(it) },
    outputSchema = this.outputSchema?.let { toJsonNode(it) },
    annotations = if (version == "none") null else mapOf("version" to TextNode(version))
)

/** Result of a tool/function call, following the MCP spec. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class McpToolResult(
    val content: JsonNode?, // The primary result (could be String, Map, etc.)
    val structuredContent: JsonNode? = null, // Optional: structured result
    val isError: String? = null, // Non-null if tool failed
    val metadata: JsonNode? = null // Optional: extra info (timings, logs, etc.)
) {
    companion object {
        fun error(error: String) = McpToolResult(content = null, isError = error)
    }
}
