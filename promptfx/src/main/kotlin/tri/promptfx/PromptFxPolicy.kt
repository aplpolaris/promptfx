/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package tri.promptfx

import javafx.scene.paint.Color
import tri.ai.core.*
import tri.ai.openai.OpenAiPlugin

/** Policy for determining which models are available within PromptFx. */
abstract class PromptFxPolicy {

    /** Returns the list of plugins that are supported by this policy. */
    abstract fun supportedPlugins(): List<TextPlugin>
    /** Returns information about all models supported by this policy. */
    abstract fun modelInfo(): List<ModelInfo>

    abstract fun embeddingModels(): List<EmbeddingModel>
    open fun embeddingModelDefault() = embeddingModels().firstOrNull() ?: EmbeddingModel.UNAVAILABLE

    abstract fun textCompletionModels(): List<TextCompletion>
    open fun textCompletionModelDefault() = textCompletionModels().firstOrNull() ?: TextCompletion.UNAVAILABLE

    abstract fun chatModels(): List<TextChat>
    open fun chatModelDefault() = chatModels().firstOrNull() ?: TextChat.UNAVAILABLE

    abstract fun multimodalModels(): List<MultimodalChat>
    open fun multimodalModelDefault() = multimodalModels().firstOrNull()

    abstract fun visionLanguageModels(): List<VisionLanguageChat>
    open fun visionLanguageModelDefault() = visionLanguageModels().first()

    abstract fun imageModels(): List<ImageGenerator>
    open fun imageModelDefault() = imageModels().firstOrNull()

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

/** Policy based on a provided plugin. */
abstract class PromptFxPolicyPlugin(val plugin: TextPlugin) : PromptFxPolicy() {
    override fun supportedPlugins() = listOf(plugin)
    override fun modelInfo() = plugin.modelInfo()
    override fun embeddingModels() = plugin.embeddingModels()
    override fun textCompletionModels() = plugin.textCompletionModels()
    override fun chatModels() = plugin.chatModels()
    override fun multimodalModels() = plugin.multimodalModels()
    override fun visionLanguageModels() = plugin.visionLanguageModels()
    override fun imageModels() = plugin.imageGeneratorModels()
}

/** OpenAI-only policy, as managed by [OpenAiPlugin]. */
object PromptFxPolicyOpenAi : PromptFxPolicyPlugin(OpenAiPlugin()) {
    override val isShowUsage = true
    override val isShowBanner = true
    override val isShowApiKeyButton = true
    override val bar = PromptFxPolicyBar("OpenAI", Color.web("#74AA9C"), Color.WHITE)
    override fun supportsView(simpleName: String) = true
}

/** Default policy, allows all registered models. */
object PromptFxPolicyUnrestricted : PromptFxPolicy() {
    override val isShowUsage = true
    override val isShowBanner = false
    override val isShowApiKeyButton = true
    override val bar = PromptFxPolicyBar("Unrestricted", Color.GRAY, Color.WHITE)
    override fun supportedPlugins() = TextPlugin.orderedPlugins
    override fun modelInfo() = TextPlugin.modelInfo()
    override fun embeddingModels() = TextPlugin.embeddingModels()
    override fun textCompletionModels() = TextPlugin.textCompletionModels()
    override fun chatModels() = TextPlugin.chatModels()
    override fun multimodalModels() = TextPlugin.multimodalModels()
    override fun visionLanguageModels() = TextPlugin.visionLanguageModels()
    override fun imageModels() = TextPlugin.imageGeneratorModels()
    override fun supportsView(simpleName: String) = true
}
