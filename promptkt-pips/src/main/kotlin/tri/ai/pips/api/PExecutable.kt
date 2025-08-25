package tri.ai.pips.api

import com.fasterxml.jackson.databind.JsonNode

/** An executable unit in a Pips pipeline. */
interface PExecutable {
    /** Tool name. */
    val name: String
    /** Tool version. */
    val version: String
    /** Schema for input. */
    val inputSchema: JsonNode?
    /** Schema for output. */
    val outputSchema: JsonNode?

    /** Execute the tool with given input and context. */
    suspend fun execute(input: JsonNode, context: PExecContext): JsonNode
}

/** Runtime context available to every executable. */
data class PExecContext(
    val vars: MutableMap<String, JsonNode> = mutableMapOf(),
    val resources: Map<String, Any?> = emptyMap(),
    val traceId: String = java.util.UUID.randomUUID().toString()
)