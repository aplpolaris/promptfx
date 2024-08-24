/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
import tri.ai.core.TextChatRole
import tri.ai.gemini.GeminiModelIndex.EMBED1
import tri.ai.gemini.GeminiModelIndex.GEMINI_PRO

@Tag("gemini")
class GeminiClientTest {

    companion object {
        lateinit var client: GeminiClient

        @JvmStatic
        @BeforeAll
        fun setUp(): Unit {
            client = GeminiClient()
        }

        @JvmStatic
        @AfterAll
        fun tearDown(): Unit {
            client.close()
        }
    }

    @Test
    fun testListModels() {
        runBlocking {
            val models = client.listModels().models
            assertTrue(models.isNotEmpty())
            println(models.joinToString("\n") { "$it" }) //"${it.name} - ${it.displayName}" })
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
            val response = client.generateContent("Write a limerick about a magic backpack.", GEMINI_PRO)
            assertNotNull(response)
            assert(response.error == null)
            println(response)
        }
    }

    @Test
    fun testGenerateContentWithChat() {
        runBlocking {
            val response = client.generateContent(listOf(
                TextChatMessage(TextChatRole.System, "You are a wizard that always responds as if you are casting a spell."),
                TextChatMessage(TextChatRole.User, "What should I have for dinner?")
            ), "gemini-1.5-pro-latest")
            assertNotNull(response)
            assert(response.error == null)
            println(response)
        }
    }

}
