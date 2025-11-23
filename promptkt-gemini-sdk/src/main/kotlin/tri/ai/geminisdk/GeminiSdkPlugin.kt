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
package tri.ai.geminisdk

import tri.ai.core.TextPlugin

/** Plugin registering models and services via the Gemini official SDK. */
class GeminiSdkPlugin : TextPlugin {

    val client = GeminiSdkClient()

    override fun modelSource() = "Gemini-SDK"

    override fun modelInfo() = 
        emptyList<tri.ai.core.ModelInfo>() // SDK doesn't provide a listModels API in the same way

    override fun embeddingModels(): List<tri.ai.core.EmbeddingModel> = 
        emptyList() // Embedding models not yet supported by the SDK wrapper

    override fun chatModels(): List<tri.ai.core.TextChat> = models(GeminiSdkModelIndex.chatModelsInclusive()) { 
        GeminiSdkTextChat(it, client) 
    }

    override fun multimodalModels() = models(GeminiSdkModelIndex.multimodalModels()) { 
        GeminiSdkMultimodalChat(it, client) 
    }

    override fun textCompletionModels() = models(
        GeminiSdkModelIndex.completionModels() + GeminiSdkModelIndex.chatModelsInclusive()
    ) {
        GeminiSdkTextCompletion(it, client)
    }

    override fun visionLanguageModels() = models(GeminiSdkModelIndex.visionLanguageModels()) { 
        GeminiSdkVisionLanguageChat(it, client) 
    }

    override fun imageGeneratorModels(): List<tri.ai.core.ImageGenerator> = 
        emptyList() // Image generation not supported by Vertex AI SDK

    override fun close() {
        client.close()
    }

    private fun <T> models(ids: List<String>, factory: (String) -> T): List<T> =
        if (!client.isConfigured()) listOf() else ids.map(factory)

}
