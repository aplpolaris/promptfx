package tri.ai.pips.core

import com.fasterxml.jackson.databind.JsonNode
import tri.ai.prompt.PromptDef
import tri.ai.prompt.template
import tri.ai.tool.wf.MAPPER

/** Fills text into a prompt template. */
class PromptExecutable(private val def: PromptDef): Executable {

    override val name: String
        get() = "prompt/${def.bareId}"
    override val description: String
        get() = def.description ?: "Prompt template ${def.bareId}"
    override val version: String
        get() = def.version ?: "0.0.0"
    override val inputSchema: JsonNode? = null
    override val outputSchema: JsonNode? = null

    @Suppress("UNCHECKED_CAST")
    override suspend fun execute(input: JsonNode, ctx: ExecContext): JsonNode {
        val args = MAPPER.convertValue(input, Map::class.java) as Map<String, Any?>
        val text = def.template().fill(args.filterValues { it != null } as Map<String, Any>)
        return MAPPER.createObjectNode().put("text", text)
    }

}