package tri.promptfx

import javafx.scene.paint.Color
import tri.ai.core.TextChat
import tri.ai.core.TextCompletion
import tri.ai.core.TextPlugin
import tri.ai.embedding.EmbeddingService
import tri.ai.openai.OpenAiTextPlugin

/** Policy for determining which models are available within PromptFx. */
abstract class PromptFxPolicy {
    abstract fun textCompletionModels(): List<TextCompletion>
    fun textCompletionModelDefault() = textCompletionModels().firstOrNull()
    abstract fun embeddingModels(): List<EmbeddingService>
    fun embeddingModelDefault() = embeddingModels().firstOrNull()
    abstract fun chatModels(): List<TextChat>
    fun chatModelDefault() = chatModels().firstOrNull()
//    abstract fun imageModels(): List<ImageService>
//    fun imageModelDefault() = imageModels().firstOrNull()

    /** Returns true if the given view is supported by this policy. */
    abstract fun supportsView(simpleName: String): Boolean

    abstract val isShowBanner: Boolean
    abstract val isShowUsage: Boolean
    abstract val isShowApiKeyButton: Boolean

    abstract val bar: PromptFxPolicyBar

    override fun toString() = javaClass.simpleName
}

/** UI components for displaying policy information. */
data class PromptFxPolicyBar(
    val text: String,
    val bgColor: Color,
    val fgColor: Color,
    val bgColorDark: Color = bgColor,
    val fgColorDark: Color = fgColor
)

/** OpenAI-only policy, as managed by [OpenAiTextPlugin]. */
object PromptFxPolicyOpenAi : PromptFxPolicy() {
    override val isShowUsage = true
    override val isShowBanner = true
    override val isShowApiKeyButton = true
    private val plugin = OpenAiTextPlugin()
    override fun textCompletionModels() = plugin.textCompletionModels()
    override fun embeddingModels() = plugin.embeddingModels()
    override fun chatModels() = plugin.chatModels()
    override val bar = PromptFxPolicyBar("OpenAI", Color.web("#74AA9C"), Color.WHITE)
    override fun supportsView(simpleName: String) = true
}

/** Default policy, allows all registered models. */
object PromptFxPolicyUnrestricted : PromptFxPolicy() {
    override val isShowUsage = true
    override val isShowBanner = false
    override val isShowApiKeyButton = true
    override fun textCompletionModels() = TextPlugin.textCompletionModels()
    override fun embeddingModels() = TextPlugin.embeddingModels()
    override fun chatModels() = TextPlugin.chatModels()
    override val bar = PromptFxPolicyBar("Unrestricted", Color.GRAY, Color.WHITE)
    override fun supportsView(simpleName: String) = true
}