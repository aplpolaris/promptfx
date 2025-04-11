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

    override fun modelSource() = "OpenAI"

    override fun modelInfo() = try {
        runBlocking {
            client.client.models().map { it.toModelInfo(modelSource()) }
        }
    } catch (x: Exception) {
        x.printStackTrace()
        emptyList()
    }

    override fun embeddingModels() =
        OpenAiAzureModelIndex.embeddingModels().map { OpenAiEmbeddingService(it, client) }

    override fun textCompletionModels() =
        OpenAiAzureModelIndex.chatModelsInclusive(false).map { OpenAiCompletionChat(it, client) } +
                OpenAiAzureModelIndex.completionModels(false).map { OpenAiCompletion(it, client) }

    override fun chatModels() =
        OpenAiAzureModelIndex.chatModelsInclusive(false).map { OpenAiChat(it, client) }

    override fun multimodalModels() =
        OpenAiAzureModelIndex.multimodalModels().map { OpenAiMultimodalChat(it, client) }

    override fun visionLanguageModels() =
        OpenAiAzureModelIndex.visionLanguageModels().map { OpenAiVisionLanguageChat(it, client) }

    override fun imageGeneratorModels() =
        OpenAiAzureModelIndex.imageGeneratorModels().map { OpenAiImageGenerator(it, client) }

    override fun close() {
        client.client.close()
    }

}

