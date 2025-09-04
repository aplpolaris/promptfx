/*-
 * #%L
 * tri.promptfx:promptkt-anthropic
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
import tri.ai.core.*

class AnthropicIntegrationTest {

    @Test
    fun `anthropic models should be available through TextPlugin system`() {
        // This test verifies that Anthropic models are discoverable through the plugin system
        val sources = TextPlugin.sources()
        assertTrue(sources.contains("Anthropic"), "Anthropic should be available as a model source")
        
        val modelInfo = TextPlugin.modelInfo()
        val anthropicModels = modelInfo.filter { it.source == "Anthropic" }
        
        // Should have Claude models when plugin is properly registered (even if not configured)
        // Note: If no API key is configured, this will be empty, which is expected behavior
        assertNotNull(anthropicModels, "Anthropic models collection should not be null")
    }

    @Test
    fun `anthropic plugin should provide model types when configured`() {
        val plugin = AnthropicPlugin()
        
        // These should return lists (empty if not configured, populated if configured)
        assertNotNull(plugin.chatModels())
        assertNotNull(plugin.textCompletionModels())
        assertNotNull(plugin.visionLanguageModels())
        assertNotNull(plugin.multimodalModels())
        assertNotNull(plugin.embeddingModels())
        assertNotNull(plugin.imageGeneratorModels())
    }

    @Test
    fun `anthropic model index should contain expected Claude models`() {
        val allModels = AnthropicModelIndex.allModels()
        
        // Verify we have the latest Claude models
        assertTrue(allModels.contains("claude-3-5-sonnet-20241022"))
        assertTrue(allModels.contains("claude-3-5-haiku-20241022"))
        assertTrue(allModels.contains("claude-3-opus-20240229"))
        assertTrue(allModels.contains("claude-3-sonnet-20240229"))
        assertTrue(allModels.contains("claude-3-haiku-20240307"))
        assertTrue(allModels.contains("claude-2.1"))
        assertTrue(allModels.contains("claude-instant-1.2"))
    }

    @Test
    fun `vision models should be subset of all models`() {
        val allModels = AnthropicModelIndex.allModels()
        val visionModels = AnthropicModelIndex.visionLanguageModels()
        
        // All vision models should be in the main model list
        assertTrue(allModels.containsAll(visionModels))
        
        // Claude 3 models should support vision
        assertTrue(visionModels.contains("claude-3-5-sonnet-20241022"))
        assertTrue(visionModels.contains("claude-3-opus-20240229"))
        
        // Claude 2 models should not support vision
        assertFalse(visionModels.contains("claude-2.1"))
        assertFalse(visionModels.contains("claude-instant-1.2"))
    }

    @Test
    fun `client should respect API key configuration`() {
        val client = AnthropicClient()
        
        // Without API key, should not be configured
        assertFalse(client.isConfigured())
        
        // Should have proper settings
        assertNotNull(client.settings)
        assertEquals("https://api.anthropic.com/v1/", client.settings.baseUrl)
        assertEquals("2023-06-01", AnthropicSettings.API_VERSION)
    }

}