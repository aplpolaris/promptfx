package tri.ai.mcp.tool

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.JsonToolExecutable
import tri.util.fine
import tri.util.json.generateJsonSchema

/** Partial implementation of an MCP tool with typed input and output. */
abstract class McpToolSupport<C : Any, D : Any>(toolType: Class<out McpToolSupport<C, D>>, val inputType: Class<C>) :
    JsonToolExecutable(findName(toolType), findDescription(toolType), generateJsonSchema(inputType.kotlin)
        .jsonPrettyPrint()) {

    override fun toString() = "${javaClass.simpleName}[name=$name, description=$description, version=$version, input=$inputSchema, output=$outputSchema]"

    override suspend fun run(input: JsonNode, context: ExecContext): String {
        var inputObj = try {
            jacksonObjectMapper().convertValue(input, inputType)
        } catch (x: IllegalArgumentException) {
            null
        }
        if (inputObj == null) {
            fine<McpToolSupport<*, *>>("Failed to parse input, trying again with an LLM.... $input")
            println("  ...  ")
            inputObj = tryConvert(input.toPrettyString(), inputType)
        }
        fine<McpToolSupport<*, *>>("Running tool: $name with input: $inputObj")
        try {
            val outputObj = run(inputObj)
            return outputObj.jsonPrettyPrint()
        } catch (x: Exception) {
            return x.message ?: "An exception occurred while running the tool."
        }
    }

    abstract suspend fun run(input: C): D

    private fun tryConvert(inputStr: String, targetType: Class<C>): C {
        // Placeholder for LLM-based conversion logic.
        // In a real implementation, this would call an LLM to convert the input string to the target type.
        throw IllegalArgumentException("LLM-based conversion not implemented.")
    }

    companion object {
        /** Find name based on [McpTool] annotation. */
        fun findName(toolType: Class<*>): String {
            val ann = toolType.getAnnotation(McpTool::class.java)
            return ann?.name?.ifBlank { toolType.simpleName } ?: toolType.simpleName
        }

        /** Find description based on [McpTool] annotation. */
        fun findDescription(toolType: Class<*>): String {
            val ann = toolType.getAnnotation(McpTool::class.java)
            return ann?.description ?: ""
        }

        private fun Any.jsonPrettyPrint() = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this)
    }
}