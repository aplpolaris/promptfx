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

import tri.ai.core.AiModelProvider

/** Plugin registering Anthropic Claude models and services via the official SDK. */
class AnthropicSdkPlugin : AiModelProvider {

    val client = AnthropicSdkClient()

    override fun isApiConfigured() = client.isConfigured()

    override fun modelSource() = AnthropicSdkModelIndex.MODEL_SOURCE

    override fun modelInfo() = emptyList<tri.ai.core.ModelInfo>()

    override fun embeddingModels() = emptyList<tri.ai.core.EmbeddingModel>()

    override fun chatModels(): List<tri.ai.core.TextChat> = models(AnthropicSdkModelIndex.chatModels()) {
        AnthropicSdkTextChat(it, client)
    }

    override fun multimodalModels(): List<tri.ai.core.MultimodalChat> = models(AnthropicSdkModelIndex.multimodalModels()) {
        AnthropicSdkMultimodalChat(it, client)
    }

    override fun textCompletionModels(): List<tri.ai.core.TextCompletion> = models(
        AnthropicSdkModelIndex.completionModels()
    ) {
        AnthropicSdkTextCompletion(it, client)
    }

    override fun imageGeneratorModels() = emptyList<tri.ai.core.ImageGenerator>()

    override fun close() {
        client.close()
    }

    private fun <T> models(ids: List<String>, factory: (String) -> T): List<T> =
        if (!client.isConfigured()) listOf() else ids.map(factory)

}
