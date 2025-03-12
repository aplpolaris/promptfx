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
package tri.ai.gemini

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import tri.ai.core.TextChatMessage
import tri.ai.core.MChatRole
import tri.ai.gemini.GeminiModelIndex.EMBED1
import tri.ai.gemini.GeminiModelIndex.GEMINI_15_FLASH
import tri.util.BASE64_AUDIO_SAMPLE

@Tag("gemini")
class GeminiClientTest {

    companion object {
        lateinit var client: GeminiClient

        @JvmStatic
        @BeforeAll
        fun setUp() {
            client = GeminiClient()
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            client.close()
        }
    }

    @Test
    fun testListModels() {
        runBlocking {
            val models = client.listModels().models
            assertTrue(models.isNotEmpty())
            println(models.joinToString("\n") { "${it.name} - ${it.displayName} - $it" })
        }
    }

    @Test
    fun testEmbedContent() {
        runBlocking {
            val response = client.embedContent("This is a test", EMBED1, outputDimensionality = 10)
            assertNotNull(response)
            println(response)
        }
    }

    @Test
    fun testBatchEmbedContents() {
        runBlocking {
            val response = client.batchEmbedContents(listOf("This is", "a test"), EMBED1, outputDimensionality = 10)
            assertNotNull(response)
            println(response)
        }
    }

    @Test
    fun testGenerateContent() {
        runBlocking {
            val response = client.generateContent("Write a limerick about a magic backpack.", GEMINI_15_FLASH)
            assertNotNull(response)
            println(response)
            with (response.candidates) {
                assert(this != null)
                assert(this!!.size == 1)
                val first = this[0]
                with (first) {
                    assert(content.role == ContentRole.model)
                    assert(content.parts.size == 1)
                    assert(content.parts[0].text != null)
                    assert(finishReason == FinishReason.STOP)
                    assert(safetyRatings == null)
                }
            }
            assert(response.promptFeedback == null)
            with (response.usageMetadata) {
                assert(this != null)
                assertEquals(10, this!!.promptTokenCount)
                assert(candidatesTokenCount > 10)
                assert(totalTokenCount > 20)
            }
            assert(response.usageMetadata != null)
        }
    }

    @Test
    fun testGenerateContentWithChat() {
        runBlocking {
            val response = client.generateContent(listOf(
                TextChatMessage(MChatRole.System, "You are a wizard that always responds as if you are casting a spell."),
                TextChatMessage(MChatRole.User, "What should I have for dinner?")
            ), GEMINI_15_FLASH)
            assertNotNull(response)
            assert(response.promptFeedback?.blockReason == null)
        }
    }

    @Test
    fun testGenerateContentAudio() {
        runBlocking {
            val request = GenerateContentRequest(Content(
                listOf(
                    Part("Transcribe this audio"),
                    Part(null, Blob.fromDataUrl(BASE64_AUDIO_SAMPLE))
                ), ContentRole.user
            ))
            val response = client.generateContent(GEMINI_15_FLASH, request)
            println(response)
            assertNotNull(response)
            assert(response.candidates!![0].content.parts[0].text != null)
        }
    }

}
