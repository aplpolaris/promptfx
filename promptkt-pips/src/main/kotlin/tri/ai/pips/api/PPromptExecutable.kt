package tri.ai.pips.api

import com.fasterxml.jackson.databind.JsonNode
import tri.ai.prompt.PromptDef
import tri.ai.prompt.PromptLibrary
import tri.ai.prompt.template
import tri.ai.tool.wf.MAPPER

/** Fills text into a prompt template. */
class PPromptExecutable(private val def: PromptDef): PExecutable {

    override val name: String
        get() = "prompt/${def.bareId}"
    override val version: String
        get() = def.version ?: "0.0.0"
    override val inputSchema: JsonNode? = null
    override val outputSchema: JsonNode? = null

    @Suppress("UNCHECKED_CAST")
    override suspend fun execute(input: JsonNode, ctx: PExecContext): JsonNode {
        val args = PPlan.MAPPER.convertValue(input, Map::class.java) as Map<String, Any?>
        val text = def.template().fill(args.filterValues { it != null } as Map<String, Any>)
        return MAPPER.createObjectNode().put("text", text)
    }

}

/** Creates an executable registry from a prompt library file. */
class PPromptLibraryExecutableRegistry(private val lib: PromptLibrary): PExecutableRegistry {

    private val executables by lazy {
        lib.list().associate { def ->
            val exec = PPromptExecutable(def)
            exec.name to exec
        }
    }

    override fun get(name: String) = executables[name]

    override fun list() = executables.values.toList()

}