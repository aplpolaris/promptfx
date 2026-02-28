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
package tri.ai.openai.api

import kotlinx.coroutines.runBlocking
import tri.ai.core.ApiSettings
import tri.ai.core.TextPlugin
import tri.ai.openai.*
import tri.util.warning

/**
 * Implementation of [TextPlugin] using OpenAI-compatible API.
 * Allows for connection to multiple API endpoints with different functionality.
 */
class OpenAiApiPlugin : TextPlugin {

    val config = OpenAiApiConfig.INSTANCE
    private val clients = mutableMapOf<OpenAiApiSettingsGeneric, OpenAiAdapter>()
    val apiSettingsList
        get() = clients.keys.toList()

    private fun client(endpoint: OpenAiApiEndpointConfig) = client(endpoint.settings)
    private fun client(settings: OpenAiApiSettingsGeneric) = clients.getOrPut(settings) {
        OpenAiAdapter(settings, settings.buildClient())
    }

    fun endpoints() = config.endpoints.map { it.source }
    fun modelSource(settings: ApiSettings) = config.endpoints.find { it.settings == settings }?.source ?: "Unknown"

    override fun isApiConfigured() = clients.keys.any { it.isConfigured() }

    override fun modelSource() = "OpenAI-Compatible API"

    override fun modelInfo() = modelInfoByEndpoint().values.flatten()

    fun modelInfoByEndpoint() = config.endpoints.mapNotNull { e ->
        try {
            runBlocking {
                e to client(e.settings).client.models().map { model ->
                    model.toModelInfo(e.source)
                }
            }
        } catch (x: Exception) {
            warning<OpenAiApiPlugin>("Failed to retrieve model info from ${e.source} - ${x.message}")
            null
        }
    }.toMap()

    override fun embeddingModels() =
        config.endpoints.flatMap { e ->
            e.index.embeddingModels().map { OpenAiEmbeddingModel(it, client(e)) }
        }

    override fun textCompletionModels() =
        config.endpoints.flatMap { e ->
            e.index.chatModelsInclusive().map { OpenAiCompletionChat(it, client(e)) } +
            e.index.completionModels().map { OpenAiCompletion(it, client(e)) }
        }

    override fun chatModels() =
        config.endpoints.flatMap { e ->
            e.index.chatModelsInclusive().map { OpenAiChat(it, client(e)) }
        }

    override fun multimodalModels() =
        config.endpoints.flatMap { e ->
            e.index.multimodalModels().map { OpenAiMultimodalChat(it, client(e)) }
        }

    override fun visionLanguageModels() =
        config.endpoints.flatMap { e ->
            e.index.visionLanguageModels().map { OpenAiVisionLanguageChat(it, client(e)) }
        }

    override fun imageGeneratorModels() =
        config.endpoints.flatMap { e ->
            e.index.imageGeneratorModels().map { OpenAiImageGenerator(it, client(e)) }
        }

    override fun close() {
        config.endpoints.forEach {
            try {
                client(it.settings).client.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}

