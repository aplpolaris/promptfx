/*-
 * #%L
 * tri.promptfx:promptfx-sample-textplugin
 * %%
 * Copyright (C) 2025 Johns Hopkins University Applied Physics Laboratory
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
package tri.promptfx.sample.textplugin

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import tri.ai.core.*
import java.util.*

class SampleTextPluginTest {

    @Test
    fun `plugin should be discoverable via ServiceLoader`() {
        val loader = ServiceLoader.load(TextPlugin::class.java)
        val plugins = loader.toList()
        
        val samplePlugin = plugins.find { it is SampleTextPlugin }
        assertNotNull(samplePlugin, "SampleTextPlugin should be discoverable via ServiceLoader")
    }

    @Test
    fun `plugin should have correct model source`() {
        val plugin = SampleTextPlugin()
        assertEquals("SampleText", plugin.modelSource())
    }

    @Test
    fun `plugin should provide model info`() {
        val plugin = SampleTextPlugin()
        val modelInfo = plugin.modelInfo()
        
        assertEquals(2, modelInfo.size)
        assertTrue(modelInfo.any { it.id == "sample-echo-v1" })
        assertTrue(modelInfo.any { it.id == "sample-chat-v1" })
    }

    @Test
    fun `plugin should provide chat model`() {
        val plugin = SampleTextPlugin()
        val chatModels = plugin.chatModels()
        
        assertEquals(1, chatModels.size)
        assertEquals("sample-chat-v1", chatModels[0].modelId)
    }

    @Test
    fun `plugin should provide text completion model`() {
        val plugin = SampleTextPlugin()
        val completionModels = plugin.textCompletionModels()
        
        assertEquals(1, completionModels.size)
        assertEquals("sample-echo-v1", completionModels[0].modelId)
    }

    @Test
    fun `text completion model should echo input`() = runBlocking {
        val model = SampleTextCompletionModel()
        val result = model.complete("Hello World", MChatVariation(), null, null, null)
        
        assertEquals("Sample Echo: Hello World", result.firstValue.text)
    }

    @Test
    fun `chat model should respond to messages`() = runBlocking {
        val model = SampleChatModel()
        val messages = listOf(
            TextChatMessage(MChatRole.User, "Test message")
        )
        val result = model.chat(messages, MChatVariation(), null, null, null, null)
        
        val response = result.firstValue.message
        assertNotNull(response)
        assertEquals(MChatRole.Assistant, response?.role)
        assertTrue(response?.content?.contains("Test message") == true)
    }

    @Test
    fun `plugin should have empty embedding models`() {
        val plugin = SampleTextPlugin()
        assertTrue(plugin.embeddingModels().isEmpty())
    }

    @Test
    fun `plugin should have empty multimodal models`() {
        val plugin = SampleTextPlugin()
        assertTrue(plugin.multimodalModels().isEmpty())
    }

    @Test
    fun `plugin should have empty vision language models`() {
        val plugin = SampleTextPlugin()
        assertTrue(plugin.visionLanguageModels().isEmpty())
    }

    @Test
    fun `plugin should have empty image generator models`() {
        val plugin = SampleTextPlugin()
        assertTrue(plugin.imageGeneratorModels().isEmpty())
    }
}
