package tri.ai.openai

import tri.ai.core.TextPlugin

/** OpenAI implementation of [TextPlugin]. */
class OpenAiTextPlugin : TextPlugin {

    val client = OpenAiClient.INSTANCE

    override fun chatModels() = listOf(
        OpenAiChat(COMBO_GPT35, client),
        OpenAiChat(COMBO_GPT4, client),
        OpenAiChat(COMBO_GPT35_16K, client)
    )

    override fun textCompletionModels() = listOf(
        OpenAiCompletionChat(COMBO_GPT35, client),
        OpenAiCompletionChat(COMBO_GPT35_16K, client),
        OpenAiCompletionChat(COMBO_GPT4, client),
        OpenAiCompletion(TEXT_ADA, client),
        OpenAiCompletion(TEXT_BABBAGE, client),
        OpenAiCompletion(TEXT_CURIE, client),
        OpenAiCompletion(TEXT_DAVINCI3, client)
    )

    override fun embeddingModels() = listOf(
        OpenAiEmbeddingService(client)
    )

}