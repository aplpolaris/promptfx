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
package tri.ai.mcp.tool

import com.fasterxml.jackson.databind.JsonNode
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.Executable
import tri.ai.core.tool.impl.WebSearchExecutable
import tri.util.json.jsonMapper

/** Provides web search via [WebSearchExecutable] and some test tools. */
class McpToolLibraryStarter: McpToolLibrary {
    var tools: List<Executable> = listOf(WebSearchExecutable()) + StubTool.loadFromResources()
    override suspend fun listTools(): List<McpToolMetadata> = tools.map { it.metadata() }

    override suspend fun getTool(name: String): McpToolMetadata? =
        tools.find { it.name == name }?.metadata()

    override suspend fun callTool(name: String, args: Map<String, Any?>): McpToolResponse {
        val tool = tools.find { it.name == name }
            ?: return McpToolResponse.error("Tool with name '$name' not found")

        val inputNode = jsonMapper.valueToTree<JsonNode>(args)
        return try {
            val outputNode = tool.execute(inputNode, ExecContext())
            McpToolResponse.createStructured(outputNode)
        } catch (e: Exception) {
            McpToolResponse.error("Error executing tool: ${e.message}")
        }
    }
}

