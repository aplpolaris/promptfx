package tri.ai.core.mm

import tri.ai.prompt.trace.AiPromptTrace

/**
 * Interface for a universal chat completion capability with a single model, designed to support multistep conversations
 * and multipart requests with combined modalities. Results may also have multiple modalities. Also supports techniques
 * for constrained outputs such as grammars, etc. Provides capacity for exceptions when models do not support a given
 * capability.
 */
interface MultimodalChat {

    /** Identifier for underlying model. */

    val modelId: String

    /** Provided a response to a sequence of chat messages. */
    suspend fun chat(
        messages: List<MultimodalChatMessage>,
        parameters: MChatParameters = MChatParameters()
    ): AiPromptTrace<MultimodalChatMessage>

    /** Provide a response to a single chat message. */
    suspend fun chat(
        message: MultimodalChatMessage,
        parameters: MChatParameters = MChatParameters()
    ): AiPromptTrace<MultimodalChatMessage> =
        chat(listOf(message), parameters)
}