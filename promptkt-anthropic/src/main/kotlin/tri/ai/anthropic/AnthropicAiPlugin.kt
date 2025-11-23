/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.anthropic

import tri.ai.core.*

/** Plugin registering models and services via the Anthropic API. */
class AnthropicAiPlugin : TextPlugin {

    val client = AnthropicClient()

    override fun modelSource() = "Anthropic"

    override fun modelInfo(): List<tri.ai.core.ModelInfo> = if (client.isConfigured())
        try {
            client.listModels().map { model ->
                tri.ai.core.ModelInfo(
                    id = model.id(),
                    type = ModelType.TEXT_CHAT,
                    source = modelSource()
                ).also {
                    // Use the model ID as the display name since displayName() returns Optional
                    it.name = model.id()
                }
            }
        } catch (x: Exception) {
            x.printStackTrace()
            emptyList()
        }
    else listOf()

    override fun embeddingModels(): List<EmbeddingModel> = models(AnthropicModelIndex.embeddingModels()) {
        TODO("Anthropic does not provide embedding models")
    }

    override fun chatModels(): List<TextChat> =
        models(AnthropicModelIndex.chatModelsInclusive()) { AnthropicTextChat(it, client) }

    override fun multimodalModels(): List<MultimodalChat> =
        models(AnthropicModelIndex.multimodalModels()) { AnthropicMultimodalChat(it, client) }

    override fun textCompletionModels(): List<TextCompletion> =
        models(AnthropicModelIndex.completionModels()) { AnthropicTextCompletion(it, client) }

    override fun visionLanguageModels(): List<VisionLanguageChat> =
        models(AnthropicModelIndex.visionLanguageModels()) { AnthropicVisionLanguageChat(it, client) }

    override fun imageGeneratorModels(): List<ImageGenerator> = models(AnthropicModelIndex.imageGeneratorModels()) {
        TODO("Anthropic does not provide image generation models")
    }

    override fun close() {
        client.close()
    }

    private fun <T> models(ids: List<String>, factory: (String) -> T): List<T> =
        if (!client.isConfigured()) listOf() else ids.map(factory)

}
