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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class GeminiSdkModelIndexTest {

    @Test
    fun testEmbeddingModels() {
        val models = GeminiSdkModelIndex.embeddingModels()
        assertTrue(models.isNotEmpty())
        assertTrue(models.contains(GeminiSdkModelIndex.EMBED4))
        println("Embedding models: $models")
    }

    @Test
    fun testMultimodalModels() {
        val models = GeminiSdkModelIndex.multimodalModels()
        assertTrue(models.isNotEmpty())
        assertTrue(models.contains(GeminiSdkModelIndex.GEMINI_15_FLASH))
        println("Multimodal models: $models")
    }

    @Test
    fun testVisionLanguageModels() {
        val models = GeminiSdkModelIndex.visionLanguageModels()
        assertTrue(models.isNotEmpty())
        assertTrue(models.contains(GeminiSdkModelIndex.GEMINI_15_FLASH))
        println("Vision language models: $models")
    }

    @Test
    fun testChatModelsInclusive() {
        val models = GeminiSdkModelIndex.chatModelsInclusive()
        assertTrue(models.isEmpty()) // No dedicated chat models, using multimodal
        println("Chat models: $models")
    }

}
