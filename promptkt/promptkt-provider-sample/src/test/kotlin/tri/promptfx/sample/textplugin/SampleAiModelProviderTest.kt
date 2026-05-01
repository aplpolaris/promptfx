/*-
 * #%L
 * tri.promptfx:promptfx-sample-textplugin
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
package tri.promptfx.sample.textplugin

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.core.*
import tri.ai.prompt.trace.AiOutput
import java.io.File
import java.util.*

class SampleAiModelProviderTest {

    private val plugin = SampleAiModelProvider()

    // --- Provider registration ---

    @Test
    fun `plugin is discoverable via ServiceLoader`() {
        val plugins = ServiceLoader.load(AiModelProvider::class.java).toList()
        assertNotNull(plugins.find { it is SampleAiModelProvider })
    }

    @Test
    fun `plugin has correct model source`() {
        assertEquals("SampleText", plugin.modelSource())
    }

    @Test
    fun `modelInfo covers all seven capability types`() {
        val types = plugin.modelInfo().map { it.type }.toSet()
        assertTrue(ModelType.TEXT_CHAT in types)
        assertTrue(ModelType.TEXT_COMPLETION in types)
        assertTrue(ModelType.TEXT_EMBEDDING in types)
        assertTrue(ModelType.IMAGE_GENERATOR in types)
        assertTrue(ModelType.TEXT_TO_SPEECH in types)
        assertTrue(ModelType.SPEECH_TO_TEXT in types)
    }

    // --- TextCompletion ---

    @Test
    fun `textCompletionModels returns one model`() {
        assertEquals(1, plugin.textCompletionModels().size)
    }

    @Test
    fun `text completion echoes input`() = runBlocking {
        val result = SampleTextCompletionModel().complete("Hello")
        assertTrue(result.exec.succeeded())
        assertEquals("Sample Echo: Hello", result.firstValue.textContent())
    }

    // --- TextChat ---

    @Test
    fun `chatModels returns one model`() {
        assertEquals(1, plugin.chatModels().size)
    }

    @Test
    fun `chat model echoes last message`() = runBlocking {
        val messages = listOf(TextChatMessage(MChatRole.User, "Test message"))
        val result = SampleChatModel().chat(messages)
        assertTrue(result.exec.succeeded())
        val reply = (result.firstValue as? AiOutput.ChatMessage)?.message
        assertNotNull(reply)
        assertEquals(MChatRole.Assistant, reply?.role)
        assertTrue(reply?.content?.contains("Test message") == true)
    }

    // --- MultimodalChat ---

    @Test
    fun `multimodalModels returns one model`() {
        assertEquals(1, plugin.multimodalModels().size)
    }

    @Test
    fun `multimodal model echoes text content`() = runBlocking {
        val message = MultimodalChatMessage.text(MChatRole.User, "Hello multimodal")
        val result = SampleMultimodalChatModel().chat(message)
        assertTrue(result.exec.succeeded())
        assertTrue(result.firstValue.textContent().contains("Hello multimodal"))
    }

    // --- EmbeddingModel ---

    @Test
    fun `embeddingModels returns one model`() {
        assertEquals(1, plugin.embeddingModels().size)
    }

    @Test
    fun `embedding model returns zero vector of correct dimensionality`() = runBlocking {
        val result = SampleEmbeddingModel().calculateEmbedding(listOf("a", "b"))
        assertEquals(2, result.size)
        assertEquals(SampleAiModelProvider.EMBEDDING_DIM, result[0].size)
        assertTrue(result[0].all { it == 0.0 })
    }

    @Test
    fun `embedding model respects custom dimensionality`() = runBlocking {
        val result = SampleEmbeddingModel().calculateEmbedding(listOf("x"), outputDimensionality = 16)
        assertEquals(16, result[0].size)
    }

    // --- ImageGenerator ---

    @Test
    fun `imageGeneratorModels returns one model`() {
        assertEquals(1, plugin.imageGeneratorModels().size)
    }

    @Test
    fun `image generator returns data URI`() = runBlocking {
        val uris = SampleImageGenerator().generateImage("a dog")
        assertEquals(1, uris.size)
        assertTrue(uris[0].toString().startsWith("data:image/png;base64,"))
    }

    @Test
    fun `image generator respects numResponses`() = runBlocking {
        val uris = SampleImageGenerator().generateImage("a cat", ImageGenerationParams(numResponses = 3))
        assertEquals(3, uris.size)
    }

    // --- TextToSpeechModel ---

    @Test
    fun `textToSpeechModels returns one model`() {
        assertEquals(1, plugin.textToSpeechModels().size)
    }

    @Test
    fun `tts model returns successful trace with byte array output`() = runBlocking {
        val result = SampleTextToSpeechModel().speech("Hello")
        assertTrue(result.exec.succeeded())
        val audio = (result.firstValue as? AiOutput.Other)?.other
        assertNotNull(audio)
        assertTrue(audio is ByteArray)
    }

    // --- SpeechToTextModel ---

    @Test
    fun `speechToTextModels returns one model`() {
        assertEquals(1, plugin.speechToTextModels().size)
    }

    @Test
    fun `stt model returns transcription containing filename`() = runBlocking {
        val file = File("test-audio.wav")
        val result = SampleSpeechToTextModel().transcribe(file)
        assertTrue(result.exec.succeeded())
        assertTrue(result.firstValue.textContent().contains("test-audio.wav"))
    }
}
