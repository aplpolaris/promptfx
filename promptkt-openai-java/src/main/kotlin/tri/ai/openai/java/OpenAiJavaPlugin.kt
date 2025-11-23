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
package tri.ai.openai.java

import kotlinx.coroutines.runBlocking
import tri.ai.core.*
import java.time.Instant

/** Plugin registering models and services via the OpenAI official Java SDK. */
class OpenAiJavaPlugin : TextPlugin {

    val client = OpenAiJavaClient()

    override fun modelSource() = "OpenAI (Java SDK)"

    override fun modelInfo() = if (client.isConfigured())
        runBlocking {
            try {
                client.listModels().map { model ->
                    ModelInfo(
                        id = model.id(),
                        type = ModelType.UNKNOWN,
                        source = modelSource()
                    ).also {
                        it.created = model.created().orElse(null)?.let { ts -> Instant.ofEpochSecond(ts).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
                    }
                }
            } catch (x: Exception) {
                x.printStackTrace()
                emptyList()
            }
        }
    else listOf()

    override fun embeddingModels() = models(
        OpenAiJavaModelIndex.embeddingModels()
    ) { OpenAiJavaEmbeddingModel(it, client) }

    override fun chatModels() = models(
        OpenAiJavaModelIndex.chatModelsInclusive()
    ) { OpenAiJavaTextChat(it, client) }

    override fun multimodalModels() = models(
        OpenAiJavaModelIndex.multimodalModels()
    ) { OpenAiJavaMultimodalChat(it, client) }

    override fun textCompletionModels() = models(
        OpenAiJavaModelIndex.chatModelsInclusive() + OpenAiJavaModelIndex.completionModels()
    ) { OpenAiJavaTextCompletion(it, client) }

    override fun visionLanguageModels() = models(
        OpenAiJavaModelIndex.visionLanguageModels()
    ) { OpenAiJavaVisionLanguageChat(it, client) }

    override fun imageGeneratorModels() = models(
        OpenAiJavaModelIndex.imageGeneratorModels()
    ) { OpenAiJavaImageGenerator(it, client) }

    override fun close() {
        client.close()
    }

    private fun <T> models(ids: List<String>, factory: (String) -> T): List<T> =
        if (!client.isConfigured()) listOf() else ids.map(factory)

}
