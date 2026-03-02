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
package tri.ai.openai.azure

import kotlinx.coroutines.runBlocking
import tri.ai.core.TextPlugin
import tri.ai.openai.*

/**
 * Implementation of [TextPlugin] using OpenAI API.
 * Models are as described in `openai-models.yaml`.
 */
class OpenAiAzurePlugin : TextPlugin {

    val client = OpenAiAzureSettings.INSTANCE

    override fun isApiConfigured() = client.settings.isConfigured()

    override fun modelSource() = OpenAiModelIndex.MODEL_SOURCE

    override fun modelInfo() = if (isApiConfigured())
        try {
            runBlocking {
                client.client.models().map { it.toModelInfo(modelSource()) }
            }
        } catch (x: Exception) {
            x.printStackTrace()
            emptyList()
        }
    else emptyList()

    override fun embeddingModels() =
        models(OpenAiAzureModelIndex.embeddingModels()) { OpenAiEmbeddingModel(it, modelSource(), client) }

    override fun textCompletionModels() =
        models(OpenAiAzureModelIndex.chatModelsInclusive(false)) { OpenAiCompletionChat(it, modelSource(), client) } +
        models(OpenAiAzureModelIndex.completionModels(false)) { OpenAiCompletion(it, modelSource(), client) }

    override fun chatModels() =
        models(OpenAiAzureModelIndex.chatModelsInclusive(false)) { OpenAiChat(it, modelSource(), client) }

    override fun multimodalModels() =
        models(OpenAiAzureModelIndex.multimodalModels()) { OpenAiMultimodalChat(it, modelSource(), client) }

    @Deprecated("Use multimodalModels() instead")
    override fun visionLanguageModels() =
        models(OpenAiAzureModelIndex.visionLanguageModels()) { OpenAiVisionLanguageChat(it, modelSource(), client) }

    override fun imageGeneratorModels() =
        models(OpenAiAzureModelIndex.imageGeneratorModels()) { OpenAiImageGenerator(it, modelSource(), client) }

    override fun close() {
        client.client.close()
    }

    private fun <T> models(ids: List<String>, factory: (String) -> T): List<T> =
        if (!client.settings.isConfigured()) listOf() else ids.map(factory)

}

