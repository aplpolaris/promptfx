/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.gemini

import kotlinx.coroutines.runBlocking
import tri.ai.core.*

/** Plugin registering models and services via the Gemini API. */
class GeminiAiPlugin : TextPlugin {

    private val client = GeminiClient()

    override fun modelSource() = "Gemini"

    override fun modelInfo() = if (client.isConfigured())
        runBlocking {
            client.listModels().models.map {
                ModelInfo(id = it.name.substringAfter("models/"), type = ModelType.UNKNOWN, source = modelSource()).apply {
                    name = it.displayName
                    version = it.version
                    description = it.description
                    inputTokenLimit = it.inputTokenLimit
                    outputTokenLimit = it.outputTokenLimit

                    params["supportedGenerationMethods"] = it.supportedGenerationMethods
                    it.baseModelId?.let { params["baseModelId"] = it }
                    it.temperature?.let { params["temperature"] = it }
                    it.topP?.let { params["topP"] = it }
                    it.topK?.let { params["topK"] = it }

                    when (it.supportedGenerationMethods.toSet()) {
                        setOf("embedContent") ->
                            type = ModelType.TEXT_EMBEDDING
                        setOf("generateContent") ->
                            type = ModelType.TEXT_CHAT
                        setOf("generateContent", "countTokens") ->
                            type = ModelType.TEXT_CHAT
                        setOf("generateContent", "countTokens", "createTunedModel") ->
                            type = ModelType.TEXT_CHAT
                        setOf("generateMessage", "countMessageTokens") -> {
                            type = ModelType.TEXT_CHAT
                            deprecation = "Deprecated PaLM API"
                        }
                        setOf("generateText", "countTextTokens", "createTunedTextModel") -> {
                            type = ModelType.TEXT_COMPLETION
                            deprecation = "Deprecated PaLM API"
                        }
                        setOf("embedText", "countTextTokens") -> {
                            type = ModelType.TEXT_EMBEDDING
                            deprecation = "Deprecated PaLM API"
                        }
                        setOf("generateAnswer") -> {
                            type = ModelType.QUESTION_ANSWER
                        }
                    }

                    if (type == ModelType.TEXT_CHAT && "vision" in id) {
                        type = ModelType.TEXT_VISION_CHAT
                    }
                }
            }
        }
    else listOf()

    override fun embeddingModels() = if (client.isConfigured())
        GeminiModelIndex.embeddingModels().map { GeminiEmbeddingService(it, client) }
    else listOf()

    override fun chatModels() = if (client.isConfigured())
        GeminiModelIndex.chatModels().map { GeminiTextChat(it, client) }
    else listOf()

    override fun textCompletionModels() = if (client.isConfigured())
        GeminiModelIndex.completionModels().map { GeminiTextCompletion(it, client) }
    else listOf()

    override fun visionLanguageModels() = if (client.isConfigured())
        GeminiModelIndex.visionLanguageModels().map { GeminiVisionLanguageChat(it, client) }
    else listOf()

    override fun imageGeneratorModels() = if (client.isConfigured())
        GeminiModelIndex.imageGeneratorModels().map { TODO() }
    else listOf()

    override fun close() {
        client.close()
    }

}
