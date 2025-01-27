package tri.ai.core.mm

import tri.ai.core.TextChatRole

data class MultimodalChatMessage(
    val role: TextChatRole,
    val content: List<MChatMessagePart>,
    val toolCalls: List<MToolCall> = listOf()
) {
    companion object {
        /** Chat message with just chat. */
        fun text(role: TextChatRole, text: String) = MultimodalChatMessage(
            role,
            listOf(MChatMessagePart(text))
        )
    }
}

/** Model parameters for multimodal chat. */
class MChatParameters(
    val variation: MChatVariation = MChatVariation(),
    val tools: MChatTools? = null,
    val tokens: Int? = 1000,
    val stop: List<String>? = null,
    val responseFormat: MultimodalResponseFormat = MultimodalResponseFormat.TEXT,
    val numResponses: Int? = null
)

/** Model parameters related to likelihood, variation, and probabilities. */
class MChatVariation(
    val seed: Int? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val frequencyPenalty: Double? = null,
    val presencePenalty: Double? = null
)

/** Model parameters related to tool use. */
class MChatTools(
    val toolChoice: MToolChoice,
    val tools: List<MTool>
)

enum class MToolChoice {
    AUTO,
    NONE
}

class MTool(
    val name: String,
    val description: String,
    val jsonSchema: String
)

/** Reference to a function to execute. */
class MToolCall(
    val id: String,
    val name: String,
    val argumentsAsJson: String
)

enum class MultimodalResponseFormat {
    JSON,
    TEXT
}

data class MChatMessagePart(
    val text: String? = null,
    // TODO - support for multiple types of inline data
    val inlineData: String? = null
)

//region BUILDERS

/** Build a [MultimodalChatMessage] from a builder. */
fun chatMessage(role: TextChatRole? = null, block: MChatMessageBuilder.() -> Unit) =
    MChatMessageBuilder().apply(block).also {
        if (role != null) it.role = role
    }.build()

/** Builder object for [MultimodalChatMessage]. */
class MChatMessageBuilder {
    var role = TextChatRole.User
    var content = mutableListOf<MChatMessagePart>()
    var params: MChatParameters? = null
    fun role(role: TextChatRole) {
        this.role = role
    }
    fun text(text: String) {
        content += MChatMessagePart(text)
    }
    fun inlineData(inlineData: String) {
        content += MChatMessagePart(inlineData = inlineData)
    }
    fun content(vararg parts: MChatMessagePart) {
        content += parts.toList()
    }
    fun content(parts: List<MChatMessagePart>) {
        content += parts
    }
    fun parameters(block: MChatParameters.() -> Unit) {
        params = MChatParameters().apply(block)
    }
    fun build() = MultimodalChatMessage(role, content)
}

//endregion