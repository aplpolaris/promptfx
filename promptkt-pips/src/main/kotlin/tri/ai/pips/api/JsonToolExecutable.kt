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
import kotlinx.serialization.json.JsonObject
import tri.ai.pips.core.ExecContext
import tri.ai.pips.core.Executable
import tri.ai.pips.core.MAPPER
import tri.ai.tool.JsonTool

/**
 * Bridge class that wraps a legacy [JsonTool] to be used as an [Executable].
 * Handles the conversion between JsonNode and JsonObject formats.
 */
class JsonToolExecutable(
    private val jsonTool: JsonTool
) : Executable {
    
    override val name: String = jsonTool.tool.name
    override val description: String = jsonTool.tool.description
    override val version: String = "1.0.0"
    override val inputSchema: JsonNode? = 
        MAPPER.readTree(jsonTool.tool.jsonSchema)
    override val outputSchema: JsonNode? = 
        MAPPER.readTree("""{"type":"object","properties":{"result":{"type":"string"}}}""")

    override suspend fun execute(input: JsonNode, context: ExecContext): JsonNode {
        // Convert JsonNode to kotlinx.serialization JsonObject
        val jsonObjectInput = convertToKotlinxJsonObject(input)
        
        // Execute the JsonTool
        val result = jsonTool.run(jsonObjectInput)
        
        // Return result as JsonNode
        return MAPPER.createObjectNode().put("result", result)
    }
    
    private fun convertToKotlinxJsonObject(input: JsonNode): JsonObject {
        // Convert Jackson JsonNode to kotlinx JsonObject
        val jsonString = MAPPER.writeValueAsString(input)
        return kotlinx.serialization.json.Json.parseToJsonElement(jsonString) as JsonObject
    }
    
    companion object {
        /** Creates a JsonToolExecutable wrapper for the given JsonTool. */
        fun wrap(jsonTool: JsonTool): JsonToolExecutable = JsonToolExecutable(jsonTool)
    }
}