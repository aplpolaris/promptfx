package tri.promptfx

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.Controller
import tri.ai.core.TextChat
import tri.ai.embedding.EmbeddingService
import tri.ai.core.TextCompletion
import tri.ai.core.TextPlugin
import tri.ai.pips.UsageUnit

/** Controller for [PromptFx]. */
class PromptFxController : Controller() {

    val openAiPlugin = TextPlugin.defaultPlugin

    val completionEngine: SimpleObjectProperty<TextCompletion> =
        SimpleObjectProperty(TextPlugin.textCompletionModels().first())
    val chatService: SimpleObjectProperty<TextChat> =
        SimpleObjectProperty(TextPlugin.chatModels().first())
    val embeddingService: SimpleObjectProperty<EmbeddingService> =
        SimpleObjectProperty(TextPlugin.embeddingModels().first())

    val tokensUsed = SimpleIntegerProperty(0)
    val audioUsed = SimpleIntegerProperty(0)
    val imagesUsed = SimpleIntegerProperty(0)

    /** Update usage stats for the OpenAI endpoint. */
    fun updateUsage() {
        tokensUsed.value = openAiPlugin.client.usage[UsageUnit.TOKENS] ?: 0
        audioUsed.value = openAiPlugin.client.usage[UsageUnit.AUDIO_MINUTES] ?: 0
        imagesUsed.value = openAiPlugin.client.usage[UsageUnit.IMAGES] ?: 0
    }

}