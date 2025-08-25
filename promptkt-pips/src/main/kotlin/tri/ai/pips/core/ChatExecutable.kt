package tri.ai.pips.core

import com.fasterxml.jackson.databind.JsonNode
import tri.ai.core.CompletionBuilder
import tri.ai.core.TextChat
import tri.ai.tool.wf.MAPPER

/** Executes for simple text input/output using a chat service. */
class ChatExecutable(val chat: TextChat): Executable {
    override val name = "chat/${chat.modelId}"
    override val description = "Chat using model ${chat.modelId}"
    override val version = "0.0.0"
    override val inputSchema: JsonNode? = null
    override val outputSchema: JsonNode? = null

    override suspend fun execute(input: JsonNode, context: ExecContext): JsonNode {
        val result = CompletionBuilder()
            .text(input.extractText())
            .execute(chat)
        return MAPPER.createObjectNode().put("message", result.firstValue.content)
    }

    private fun JsonNode.extractText(): String = when {
        isTextual -> asText()
        has("message") -> get("message").extractText()
        has("text") -> get("text").extractText()
        else -> toString()
    }

}