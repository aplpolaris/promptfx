package tri.ai.pips.core

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

/** Runtime context available to every executable. */
data class ExecContext(
    val vars: MutableMap<String, JsonNode> = mutableMapOf(),
    val resources: Map<String, Any?> = emptyMap(),
    val traceId: String = UUID.randomUUID().toString()
)