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
package tri.ai.openai

import kotlinx.coroutines.runBlocking
import tri.ai.core.TextPlugin

/**
 * Implementation of [TextPlugin] using OpenAI API.
 * Models are as described in `openai-models.yaml`.
 */
class OpenAiPlugin : TextPlugin {

    val client = OpenAiAdapter.INSTANCE

    override fun isApiConfigured() = client.settings.isConfigured()

    override fun modelSource() = OpenAiModelIndex.MODEL_SOURCE

    override fun modelInfo() = if (client.settings.isConfigured())
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
        models(OpenAiModelIndex.embeddingModels()) { OpenAiEmbeddingModel(it, modelSource(), client) }

    override fun textCompletionModels() =
        models(OpenAiModelIndex.chatModelsInclusive(false)) { OpenAiCompletionChat(it, modelSource(), client) } +
        models(OpenAiModelIndex.completionModels(false)) { OpenAiCompletion(it, modelSource(), client) }

    override fun chatModels() =
        models(OpenAiModelIndex.chatModelsInclusive(false)) { OpenAiChat(it, modelSource(), client) }

    override fun multimodalModels() =
        models(OpenAiModelIndex.multimodalModels()) { OpenAiMultimodalChat(it, modelSource(), client) }

    override fun visionLanguageModels() =
        models(OpenAiModelIndex.visionLanguageModels()) { OpenAiVisionLanguageChat(it, modelSource(), client) }

    override fun imageGeneratorModels() =
        models(OpenAiModelIndex.imageGeneratorModels()) { OpenAiImageGenerator(it, modelSource(), client) }

    override fun close() {
        client.client.close()
    }

    private fun <T> models(ids: List<String>, factory: (String) -> T): List<T> =
        if (!client.settings.isConfigured()) listOf() else ids.map(factory)

}

