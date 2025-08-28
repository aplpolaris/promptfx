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

/**
 * Implementation of [TextPlugin] using Anthropic API.
 * Models are as described in `anthropic-models.yaml`.
 */
class AnthropicPlugin : TextPlugin {

    val client = AnthropicAdapter.INSTANCE

    override fun modelSource() = "Anthropic"

    override fun modelInfo() = try {
        // Anthropic doesn't have a list models API, so we return the predefined models
        AnthropicModelIndex.modelInfoIndex.values.toList()
    } catch (x: Exception) {
        x.printStackTrace()
        emptyList()
    }

    override fun embeddingModels() = 
        // Anthropic doesn't provide embedding models
        emptyList<EmbeddingModel>()

    override fun textCompletionModels() =
        // Anthropic uses chat completions for text completion
        AnthropicModelIndex.chatModelsInclusive(false).map { AnthropicChat(it, client) } as List<TextCompletion>

    override fun chatModels() =
        AnthropicModelIndex.chatModelsInclusive(false).map { AnthropicChat(it, client) }

    override fun multimodalModels() =
        AnthropicModelIndex.multimodalModels().map { AnthropicMultimodalChat(it, client) }

    override fun visionLanguageModels() =
        AnthropicModelIndex.visionLanguageModels().map { AnthropicVisionLanguageChat(it, client) }

    override fun imageGeneratorModels() =
        // Anthropic doesn't provide image generation
        emptyList<ImageGenerator>()

    override fun close() {
        client.client.close()
    }

}