package tri.promptfx

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.Controller
import tri.ai.embedding.EmbeddingService
import tri.ai.openai.*
import tri.ai.core.TextCompletion
import tri.ai.pips.UsageUnit

/** Controller for [PromptFx]. */
class PromptFxController : Controller() {

    val openAiClient = OpenAiClient.INSTANCE

    val completionEngineOptions = listOf(
        OpenAiCompletionChat(COMBO_GPT35, openAiClient),
        OpenAiCompletionChat(COMBO_GPT4, openAiClient),
        OpenAiCompletion(TEXT_ADA, openAiClient),
        OpenAiCompletion(TEXT_BABBAGE, openAiClient),
        OpenAiCompletion(TEXT_CURIE, openAiClient),
        OpenAiCompletion(TEXT_DAVINCI3, openAiClient),
    )

    val completionEngine: SimpleObjectProperty<TextCompletion> =
        SimpleObjectProperty(completionEngineOptions.first())
    val embeddingService: SimpleObjectProperty<EmbeddingService> =
        SimpleObjectProperty(OpenAiEmbeddingService(openAiClient))

    val tokensUsed = SimpleIntegerProperty(0)
    val audioUsed = SimpleIntegerProperty(0)
    val imagesUsed = SimpleIntegerProperty(0)

    /** Update usage stats for the OpenAI endpoint. */
    fun updateUsage() {
        tokensUsed.value = openAiClient.usage[UsageUnit.TOKENS] ?: 0
        audioUsed.value = openAiClient.usage[UsageUnit.AUDIO_MINUTES] ?: 0
        imagesUsed.value = openAiClient.usage[UsageUnit.IMAGES] ?: 0
    }

}