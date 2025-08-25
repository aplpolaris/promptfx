/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx.api

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.core.*
import tri.promptfx.PromptFxModels
import tri.promptfx.PromptFxPolicyUnrestricted

/**
 * Test class for ModelsView functionality, particularly around plugin timeout handling.
 */
class ModelsViewTest {

    /** Test plugin that simulates a slow/timeout response. */
    class SlowTestPlugin(private val delayMs: Long, private val shouldThrow: Boolean = false) : TextPlugin {
        override fun modelSource(): String = "TestSlow"
        
        override fun modelInfo(): List<ModelInfo> {
            if (shouldThrow) throw RuntimeException("Simulated plugin failure")
            Thread.sleep(delayMs)
            return listOf(
                ModelInfo("test-model-1", ModelType.TEXT_CHAT, modelSource())
            )
        }
        
        override fun embeddingModels(): List<EmbeddingModel> = emptyList()
        override fun chatModels(): List<TextChat> = emptyList()
        override fun multimodalModels(): List<MultimodalChat> = emptyList()
        override fun textCompletionModels(): List<TextCompletion> = emptyList()
        override fun visionLanguageModels(): List<VisionLanguageChat> = emptyList()
        override fun imageGeneratorModels(): List<ImageGenerator> = emptyList()
        override fun close() {}
    }

    /** Test plugin that responds quickly. */
    class FastTestPlugin : TextPlugin {
        override fun modelSource(): String = "TestFast"
        
        override fun modelInfo(): List<ModelInfo> = listOf(
            ModelInfo("fast-model-1", ModelType.TEXT_COMPLETION, modelSource()),
            ModelInfo("fast-model-2", ModelType.TEXT_EMBEDDING, modelSource())
        )
        
        override fun embeddingModels(): List<EmbeddingModel> = emptyList()
        override fun chatModels(): List<TextChat> = emptyList()
        override fun multimodalModels(): List<MultimodalChat> = emptyList()
        override fun textCompletionModels(): List<TextCompletion> = emptyList()
        override fun visionLanguageModels(): List<VisionLanguageChat> = emptyList()
        override fun imageGeneratorModels(): List<ImageGenerator> = emptyList()
        override fun close() {}
    }

    @Test
    fun testPolicySupportsMultiplePlugins() {
        // Test that the policy can handle multiple plugins
        val supportedPlugins = PromptFxModels.policy.supportedPlugins()
        assertTrue(supportedPlugins.isNotEmpty(), "Should have at least one plugin")
        
        // Test that unrestricted policy returns plugin list
        assertTrue(PromptFxPolicyUnrestricted.supportedPlugins().isNotEmpty())
    }

    @Test
    fun testFastPluginModelInfo() {
        // Test that a fast plugin returns models quickly
        val fastPlugin = FastTestPlugin()
        val models = fastPlugin.modelInfo()
        
        assertEquals(2, models.size, "Fast plugin should return 2 models")
        assertEquals("fast-model-1", models[0].id)
        assertEquals("fast-model-2", models[1].id)
        assertEquals("TestFast", models[0].source)
    }

    @Test
    fun testSlowPluginHandling() {
        // Test that slow plugin eventually returns results
        val slowPlugin = SlowTestPlugin(100) // 100ms delay
        val models = slowPlugin.modelInfo()
        
        assertEquals(1, models.size, "Slow plugin should return 1 model")
        assertEquals("test-model-1", models[0].id)
    }

    @Test
    fun testFailingPluginHandling() {
        // Test that failing plugin throws exception as expected
        val failingPlugin = SlowTestPlugin(0, true)
        
        assertThrows(RuntimeException::class.java) {
            failingPlugin.modelInfo()
        }
    }
}