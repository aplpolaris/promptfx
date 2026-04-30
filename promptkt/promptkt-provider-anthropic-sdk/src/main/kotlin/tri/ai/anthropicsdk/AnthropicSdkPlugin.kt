/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.anthropicsdk

import tri.ai.core.*
import tri.util.warning

/** Plugin registering Anthropic Claude models and services via the official SDK. */
class AnthropicSdkPlugin : AiModelProvider {

    val client = AnthropicSdkClient()

    override fun isApiConfigured() = client.isConfigured()

    override fun modelSource() = AnthropicSdkModelIndex.MODEL_SOURCE

    override fun modelInfo(): List<ModelInfo> {
        val allIds = (AnthropicSdkModelIndex.chatModels() +
                AnthropicSdkModelIndex.completionModels() +
                AnthropicSdkModelIndex.multimodalModels()).distinct()
        return allIds.map { id ->
            AnthropicSdkModelIndex.modelInfoIndex[id] ?: run {
                warning<AnthropicSdkPlugin>("Model info not found for '$id'; using default capabilities.")
                ModelInfo(id, ModelType.TEXT_VISION_CHAT, modelSource()).also {
                    it.capabilities.inputs = listOf(DataModality.text, DataModality.image)
                    it.capabilities.outputs = listOf(DataModality.text)
                }
            }
        }
    }

    override fun embeddingModels() = emptyList<EmbeddingModel>()

    override fun chatModels(): List<TextChat> = models(AnthropicSdkModelIndex.chatModels()) {
        AnthropicSdkTextChat(it, client)
    }

    override fun multimodalModels(): List<MultimodalChat> = models(AnthropicSdkModelIndex.multimodalModels()) {
        AnthropicSdkMultimodalChat(it, client)
    }

    override fun textCompletionModels(): List<TextCompletion> = models(
        AnthropicSdkModelIndex.completionModels()
    ) {
        AnthropicSdkTextCompletion(it, client)
    }

    override fun imageGeneratorModels() = emptyList<ImageGenerator>()

    override fun close() {
        client.close()
    }

    private fun <T> models(ids: List<String>, factory: (String) -> T): List<T> =
        if (!client.isConfigured()) listOf() else ids.map(factory)

}
