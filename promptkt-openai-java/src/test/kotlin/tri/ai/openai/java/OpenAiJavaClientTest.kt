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
package tri.ai.openai.java

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import tri.ai.core.TextChatMessage
import tri.ai.core.MChatRole
import tri.ai.core.MChatVariation
import tri.ai.openai.java.OpenAiJavaModelIndex.GPT_4O_MINI

@Tag("openai")
class OpenAiJavaClientTest {

    companion object {
        lateinit var client: OpenAiJavaClient

        @JvmStatic
        @BeforeAll
        fun setUp() {
            client = OpenAiJavaClient()
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
            val models = client.listModels()
            assertTrue(models.isNotEmpty())
            println("Available models: ${models.map { it.id() }}")
        }
    }

    @Test
    fun testChatCompletion() {
        runBlocking {
            val chat = OpenAiJavaTextChat(GPT_4O_MINI, client)
            val response = chat.chat(
                listOf(TextChatMessage(MChatRole.User, "Say 'Hello, World!' and nothing else.")),
                MChatVariation(),
                tokens = 50
            )
            assertNotNull(response)
            println(response)
            assertTrue(response.output != null)
        }
    }

    @Test
    fun testEmbedding() {
        runBlocking {
            val embedding = OpenAiJavaEmbeddingModel(OpenAiJavaModelIndex.EMBEDDING_3_SMALL, client)
            val response = embedding.calculateEmbedding(listOf("This is a test"), outputDimensionality = null)
            assertNotNull(response)
            assertEquals(1, response.size)
            assertTrue(response[0].isNotEmpty())
            println("Embedding dimension: ${response[0].size}")
        }
    }

}
