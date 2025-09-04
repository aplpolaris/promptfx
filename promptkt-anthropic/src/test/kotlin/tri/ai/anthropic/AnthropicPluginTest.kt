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
import tri.ai.core.TextPlugin
import java.util.*

class AnthropicPluginTest {

    @Test
    fun `plugin should be discoverable via ServiceLoader`() {
        val loader = ServiceLoader.load(TextPlugin::class.java)
        val plugins = loader.toList()
        
        val anthropicPlugin = plugins.find { it is AnthropicPlugin }
        assertNotNull(anthropicPlugin, "AnthropicPlugin should be discoverable via ServiceLoader")
    }

    @Test
    fun `plugin should have correct properties`() {
        val plugin = AnthropicPlugin()
        
        assertEquals("Anthropic", plugin.modelSource())
        assertNotNull(plugin.chatModels())
        assertNotNull(plugin.textCompletionModels())
        assertNotNull(plugin.multimodalModels())
        assertNotNull(plugin.visionLanguageModels())
    }

    @Test
    fun `plugin should return Claude models when configured`() {
        val plugin = AnthropicPlugin()
        
        // Test that model index returns expected models
        val chatModels = AnthropicModelIndex.chatModels()
        assertTrue(chatModels.contains("claude-3-5-sonnet-20241022"))
        assertTrue(chatModels.contains("claude-3-opus-20240229"))
        assertTrue(chatModels.contains("claude-2.1"))
    }

    @Test
    fun `client should handle missing API key gracefully`() {
        val client = AnthropicClient()
        
        // Should not be configured without API key
        assertFalse(client.isConfigured())
    }

}