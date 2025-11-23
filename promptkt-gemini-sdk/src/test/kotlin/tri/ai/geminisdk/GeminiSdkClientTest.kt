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
package tri.ai.geminisdk

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import tri.ai.core.TextChatMessage
import tri.ai.core.MChatRole
import tri.ai.core.MChatVariation
import tri.ai.geminisdk.GeminiSdkModelIndex.EMBED4
import tri.ai.geminisdk.GeminiSdkModelIndex.GEMINI_15_FLASH

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
            println("Client is configured with project ID: ${client.settings.projectId}")
            println("Location: ${client.settings.location}")
        } else {
            println("Client is not configured. Set GEMINI_API_KEY and GEMINI_PROJECT_ID to run tests.")
        }
    }

    @Test
    fun testEmbedContentNotImplemented() {
        Assumptions.assumeTrue(client.isConfigured(), "Client not configured")
        
        runBlocking {
            assertThrows<NotImplementedError> {
                client.embedContents(listOf("This is a test"), EMBED4)
            }
        }
    }

    @Test
    fun testGenerateContent() {
        Assumptions.assumeTrue(client.isConfigured(), "Client not configured")
        
        runBlocking {
            val response = client.generateContent(
                "Write a short limerick about a magic backpack.",
                GEMINI_15_FLASH,
                MChatVariation(),
                history = emptyList()
            )
            assertNotNull(response)
            val responseText = response.text()
            assertNotNull(responseText)
            assertTrue(responseText!!.isNotBlank())
            println("Generated response: $responseText")
        }
    }

    @Test
    fun testGenerateContentWithHistory() {
        Assumptions.assumeTrue(client.isConfigured(), "Client not configured")
        
        runBlocking {
            val history = listOf(
                TextChatMessage(MChatRole.System, "You are a helpful assistant that responds concisely."),
                TextChatMessage(MChatRole.User, "What is 2+2?")
            )
            val response = client.generateContent(
                "And what is 3+3?",
                GEMINI_15_FLASH,
                MChatVariation(),
                history = history
            )
            assertNotNull(response)
            val responseText = response.text()
            assertNotNull(responseText)
            println("Chat response: $responseText")
        }
    }

}
