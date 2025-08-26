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
package tri.ai.tool

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import tri.ai.pips.core.ExecContext
import tri.ai.pips.core.Executable
import tri.ai.pips.core.MAPPER

/**
 * Base class for JSON schema-based executables.
 * This replaces the deprecated JsonTool class by implementing Executable directly.
 */
abstract class JsonToolExecutable(
    override val name: String,
    override val description: String,
    private val jsonSchema: String,
    override val version: String = "1.0.0"
) : Executable {

    override val inputSchema: JsonNode by lazy { MAPPER.readTree(jsonSchema) }
    override val outputSchema: JsonNode by lazy { MAPPER.readTree(OUTPUT_SCHEMA) }

    override suspend fun execute(input: JsonNode, context: ExecContext): JsonNode {
        val jsonObjectInput = convertToKotlinxJsonObject(input)
        val result = run(jsonObjectInput, context)
        return context.mapper.createObjectNode().put("result", result)
    }

    /**
     * Execute the tool with JsonObject input and context.
     * Returns a string result.
     */
    abstract suspend fun run(input: JsonObject, context: ExecContext): String

    companion object {
        private const val OUTPUT_SCHEMA = """{"type":"object","properties":{"result":{"type":"string"}}}"""

        fun convertToKotlinxJsonObject(input: JsonNode): JsonObject {
            val jsonString = MAPPER.writeValueAsString(input)
            return Json.parseToJsonElement(jsonString) as JsonObject
        }
    }
}
