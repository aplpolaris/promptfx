/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.geminisdk

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.MChatRole
import tri.ai.core.MChatVariation
import tri.ai.core.MultimodalChatMessage
import tri.ai.geminisdk.GeminiSdkModelIndex.GEMINI_EMBEDDING
import tri.ai.geminisdk.GeminiSdkModelIndex.GEMINI_25_FLASH_LITE

@Tag("gemini-sdk")
class GeminiSdkClientTest {

    companion object {
        lateinit var client: GeminiSdkClient

        @JvmStatic
        @BeforeAll
        fun setUp() {
            client = GeminiSdkClient()
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            client.close()
        }
    }

    @Test
    fun testClientConfiguration() {
        if (client.isConfigured()) {
            if (client.settings.projectId.isBlank()) {
                println("Warning: Client is configured without a project ID. Using Gemini Developer API mode.")
            } else {
                println("Client is configured in Vertex AI mode.")
                println("Client is configured with project ID: ${client.settings.projectId}")
            }
            println("Location: ${client.settings.location}")
        } else {
            println("Client is not configured. Set GEMINI_API_KEY and GEMINI_PROJECT_ID to run tests.")
        }
    }

    @Test
    fun testEmbedContent() {
        runBlocking {
            val response = client.embedContent("This is a test", GEMINI_EMBEDDING, outputDimensionality = 10)
            assertNotNull(response)
            println(response)
        }
    }

    @Test
    fun testBatchEmbedContents() {
        runBlocking {
            val response = client.batchEmbedContents(listOf("This is", "a test"), GEMINI_EMBEDDING, outputDimensionality = 10)
            assertNotNull(response)
            println(response)
        }
    }

    @Test
    fun testGenerateContent() {
        Assumptions.assumeTrue(client.isConfigured(), "Client not configured")
        
        runBlocking {
            val response = client.generateContent(
                GEMINI_25_FLASH_LITE,
                MChatVariation(),
                listOf(MultimodalChatMessage.text(MChatRole.User, "Write a short limerick about a magic backpack."))
            )
            assertNotNull(response)
            val responseText = response.text()
            assertNotNull(responseText)
            assertTrue(responseText!!.isNotBlank())
            println(responseText)
            assertTrue("pack" in responseText.lowercase(), "Response should mention 'backpack'")
        }
    }

    @Test
    fun testGenerateContentWithHistory() {
        Assumptions.assumeTrue(client.isConfigured(), "Client not configured")
        
        runBlocking {
            val history = listOf(
                MultimodalChatMessage.text(MChatRole.System, "You are a helpful assistant that responds concisely."),
                MultimodalChatMessage.text(MChatRole.User, "What is 2+2?"),
                MultimodalChatMessage.text(MChatRole.System, "The answer is 4."),
                MultimodalChatMessage.text(MChatRole.User, "And what is 3 more than that?")
            )
            val response = client.generateContent(
                GEMINI_25_FLASH_LITE,
                MChatVariation(),
                history = history
            )
            assertNotNull(response)
            val responseText = response.text()
            assertNotNull(responseText)
            println("Chat response: $responseText")
            assertTrue(responseText!!.contains("7") || responseText.contains("seven"), "Response should contain the answer '7'")
        }
    }

}
