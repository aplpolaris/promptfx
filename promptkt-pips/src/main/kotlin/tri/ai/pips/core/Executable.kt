package tri.ai.pips.core

import com.fasterxml.jackson.databind.JsonNode

/**
 * An executable unit in a Pips pipeline, such as a tool or an AI model.
 */
interface Executable {
    /** Tool name. */
    val name: String
    /** Tool description. */
    val description: String
    /** Tool version. */
    val version: String
    /** Schema for input. */
    val inputSchema: JsonNode?
    /** Schema for output. */
    val outputSchema: JsonNode?

    /** Execute the tool with given input and context. */
    suspend fun execute(input: JsonNode, context: ExecContext): JsonNode
}