/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
import tri.ai.core.TextChat
import tri.ai.core.TextCompletion
import tri.ai.core.TextPlugin
import tri.ai.embedding.EmbeddingService
import tri.ai.openai.OpenAiTextPlugin

/** Policy for determining which models are available within PromptFx. */
abstract class PromptFxPolicy {
    abstract fun textCompletionModels(): List<TextCompletion>
    fun textCompletionModelDefault() = textCompletionModels().first()
    abstract fun embeddingModels(): List<EmbeddingService>
    fun embeddingModelDefault() = embeddingModels().first()
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
