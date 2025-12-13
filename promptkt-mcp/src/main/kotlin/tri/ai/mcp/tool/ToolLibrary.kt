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
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.Executable
import tri.ai.core.tool.impl.WebSearchExecutable
import tri.ai.mcp.JsonSerializers.toJsonElement
import tri.util.json.buildSchemaWithOneOptionalParam
import tri.util.json.buildSchemaWithOneRequiredParam
import tri.util.json.jsonMapper
import kotlin.String

interface ToolLibrary {
    suspend fun listTools(): List<Executable>
    suspend fun getTool(name: String): Executable?
    suspend fun callTool(name: String, args: Map<String, String>): McpToolResult
}

class StarterToolLibrary: ToolLibrary {
    var tools: List<Executable> = listOf(WebSearchExecutable()) + FakeTools.load()
    override suspend fun listTools(): List<Executable> = tools

    override suspend fun getTool(name: String): Executable? =
        tools.find { it.name == name }

    override suspend fun callTool(name: String, args: Map<String, String>): McpToolResult {
        val tool = getTool(name)
            ?: return McpToolResult.error(name, "Tool with name '$name' not found")

        val inputNode = jsonMapper.valueToTree<JsonNode>(args)
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
