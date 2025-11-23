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
package tri.ai.anthropic

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class AnthropicModelIndexTest {

    @Test
    fun testChatModels() {
        val models = AnthropicModelIndex.chatModels()
        assertTrue(models.isNotEmpty())
        assertTrue(models.contains(AnthropicModelIndex.CLAUDE_3_5_SONNET_20241022))
        println("Chat models: $models")
    }

    @Test
    fun testMultimodalModels() {
        val models = AnthropicModelIndex.multimodalModels()
        assertTrue(models.isNotEmpty())
        assertTrue(models.contains(AnthropicModelIndex.CLAUDE_3_OPUS_20240229))
        println("Multimodal models: $models")
    }

    @Test
    fun testEmbeddingModels() {
        val models = AnthropicModelIndex.embeddingModels()
        assertTrue(models.isEmpty())
        println("Embedding models: $models")
    }

}
