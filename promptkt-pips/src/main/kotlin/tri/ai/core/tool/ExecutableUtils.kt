package tri.ai.core.tool

import kotlinx.serialization.SerializationException
import tri.ai.core.MTool
import tri.ai.core.agent.MAPPER
import tri.ai.core.agent.impl.JsonToolExecutor
import tri.util.warning

/** Creates an [MTool] from a JSON schema. */
fun Executable.createTool(): MTool? {
    val schema = try {
        inputSchema?.let { MAPPER.writeValueAsString(it) }
    } catch (x: SerializationException) {
        warning<JsonToolExecutor>("Invalid JSON schema: $inputSchema", x)
        null
    }
    return schema?.let { MTool(name, description, it) }
}