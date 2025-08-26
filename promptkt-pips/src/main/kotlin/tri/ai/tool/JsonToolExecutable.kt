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

        private fun convertToKotlinxJsonObject(input: JsonNode): JsonObject {
            val jsonString = MAPPER.writeValueAsString(input)
            return Json.parseToJsonElement(jsonString) as JsonObject
        }
    }
}