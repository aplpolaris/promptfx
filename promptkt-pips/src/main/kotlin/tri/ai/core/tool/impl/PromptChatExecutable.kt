package tri.ai.core.tool.impl

import com.fasterxml.jackson.databind.JsonNode
import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.core.agent.createObject
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.Executable
import tri.ai.prompt.PromptDef

/** Fills text into a prompt template, and generates a response using an LLM. */
class PromptChatExecutable(private val def: PromptDef, private val chatExec: TextChat): Executable {

    override val name: String
        get() = "prompt_chat/${def.bareId}"
    override val description: String
        get() = def.description ?: "Prompt template chat ${def.bareId}"
    override val version: String
        get() = def.version ?: "0.0.0"
    override val inputSchema: JsonNode? = null
    override val outputSchema: JsonNode? = null

    @Suppress("UNCHECKED_CAST")
    override suspend fun execute(input: JsonNode, ctx: ExecContext): JsonNode {
        val filled = PromptFillExecutable(def).execute(input, ctx)
        val response = chatExec.chat(listOf(TextChatMessage.Companion.user(filled.get(PromptFillExecutable.Companion.TEXT_KEY).asText())))
            .firstValue.textContent()
        return createObject(RESPONSE_KEY, response)
    }

    companion object {
        private const val RESPONSE_KEY = "response"
    }

}