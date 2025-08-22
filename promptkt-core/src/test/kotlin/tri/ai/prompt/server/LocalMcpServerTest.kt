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
package tri.ai.prompt.server

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import tri.ai.core.MChatMessagePart
import tri.ai.core.MPartType
import tri.ai.prompt.PromptLibrary

class LocalMcpServerTest {

    @Test
    fun testListPrompts() {
        runTest {
            val server = LocalMcpServer(PromptLibrary.INSTANCE)
            val prompts = server.listPrompts()
            println(prompts)
            assertTrue(prompts.isNotEmpty(), "Should have at least one prompt")
            server.close()
        }
    }

    @Test
    fun testGetPrompt() {
        runTest {
            val server = LocalMcpServer(PromptLibrary.INSTANCE)

            // Test getting a prompt that exists
            val firstPrompt = server.listPrompts().first()
            val response = server.getPrompt(firstPrompt.id)
            println(response)
            assertNotNull(response)
            assertFalse(response.messages.isEmpty())

            // Test getting a prompt that doesn't exist
            try {
                server.getPrompt("nonexistent-prompt")
                fail("Expected McpServerException")
            } catch (e: McpServerException) {
                // Expected
            }

            server.close()
        }
    }

    @Test
    fun testGetPrompt_arguments() {
        runTest {
            val server = LocalMcpServer(PromptLibrary.INSTANCE)

            val response = server.getPrompt("text-qa/answer", mapOf(
                "instruct" to "What is the meaning of life?",
                "input" to "42"
            ))
            println(response)
            assertEquals("Question Answer Example", response.description)
            assertEquals(1, response.messages.size)
            assertEquals(1, response.messages[0].content!!.size)
            assertEquals(listOf(MChatMessagePart(
                partType = MPartType.TEXT,
                text = """
                Answer the following question about the text below. If you do not know the answer, say you don't know.
                ```
                42
                ```
                Question: What is the meaning of life?
                Answer:
                
            """.trimIndent()
            )), response.messages[0].content)

            server.close()
        }
    }

    @Test
    fun testCapabilities() {
        runTest {
            val adapter = LocalMcpServer(PromptLibrary.INSTANCE)

            val capabilities = adapter.getCapabilities()
            println(capabilities)
            assertNotNull(capabilities)
            assertNotNull(capabilities.prompts)

            adapter.close()
        }
    }

}
