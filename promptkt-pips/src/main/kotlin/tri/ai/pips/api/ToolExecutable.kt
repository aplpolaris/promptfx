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
package tri.ai.pips.api

import com.fasterxml.jackson.databind.JsonNode
import tri.ai.pips.core.ExecContext
import tri.ai.pips.core.Executable
import tri.ai.pips.core.MAPPER
import tri.ai.tool.Tool
import tri.ai.tool.TOOL_DICT_INPUT

/**
 * Bridge class that wraps a legacy [Tool] to be used as an [Executable].
 * Converts JsonNode input/output to the Tool's ToolDict format.
 */
class ToolExecutable(
    private val tool: Tool
) : Executable {
    
    override val name: String = tool.name
    override val description: String = tool.description
    override val version: String = "1.0.0"
    override val inputSchema: JsonNode? = null
    override val outputSchema: JsonNode? = null

    override suspend fun execute(input: JsonNode, context: ExecContext): JsonNode {
        // Extract input text from JsonNode
        val inputText = when {
            input.isTextual -> input.asText()
            input.has("input") -> input.get("input").asText()
            input.has("request") -> input.get("request").asText()
            input.has("text") -> input.get("text").asText()
            else -> input.toString()
        }
        
        // Create ToolDict input
        val toolDict = mapOf(TOOL_DICT_INPUT to inputText)
        
        // Execute the tool
        val result = tool.run(toolDict)
        
        // Convert result back to JsonNode
        return MAPPER.createObjectNode().apply {
            put("result", result.finalResult ?: result.result[tri.ai.tool.TOOL_DICT_RESULT] ?: "")
            put("isTerminal", result.isTerminal)
        }
    }
    
    companion object {
        /** Creates a ToolExecutable wrapper for the given Tool. */
        fun wrap(tool: Tool): ToolExecutable = ToolExecutable(tool)
    }
}