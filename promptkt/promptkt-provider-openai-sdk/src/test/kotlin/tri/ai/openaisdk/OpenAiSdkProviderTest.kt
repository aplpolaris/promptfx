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
package tri.ai.openaisdk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for [OpenAiSdkAiProvider] — no API key required. */
class OpenAiSdkProviderTest {

    private val provider = OpenAiSdkAiProvider()

    @Test
    fun testModelSource() {
        assertEquals(OpenAiSdkModelIndex.MODEL_SOURCE, provider.modelSource())
    }

    @Test
    fun testModelIndexChatModelsNonEmpty() {
        val models = OpenAiSdkModelIndex.chatModels()
        assertTrue(models.isNotEmpty(), "Expected non-empty chat model list from openai-models.yaml")
        println("Chat models: $models")
    }

    @Test
    fun testModelIndexEmbeddingModelsNonEmpty() {
        val models = OpenAiSdkModelIndex.embeddingModels()
        assertTrue(models.isNotEmpty(), "Expected non-empty embedding model list from openai-models.yaml")
        println("Embedding models: $models")
    }

    @Test
    fun testModelIndexMultimodalModelsNonEmpty() {
        val models = OpenAiSdkModelIndex.multimodalModels()
        assertTrue(models.isNotEmpty(), "Expected non-empty multimodal model list from openai-models.yaml")
        println("Multimodal models: $models")
    }

    @Test
    fun testModelIndexImageGeneratorModelsNonEmpty() {
        val models = OpenAiSdkModelIndex.imageGeneratorModels()
        assertTrue(models.isNotEmpty(), "Expected non-empty image generator model list from openai-models.yaml")
        println("Image generator models: $models")
    }

    @Test
    fun testModelIndexKnownIds() {
        assertTrue(OpenAiSdkModelIndex.embeddingModels().contains(OpenAiSdkModelIndex.EMBEDDING_ADA))
        assertTrue(OpenAiSdkModelIndex.multimodalModels().contains(OpenAiSdkModelIndex.GPT4O))
        println("Known IDs verified.")
    }

}
