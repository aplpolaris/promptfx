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
package tri.ai.mcp

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tri.ai.core.MChatMessagePart
import tri.ai.core.MPartType
import tri.ai.prompt.PromptArgDef
import tri.ai.prompt.PromptDef
import tri.ai.prompt.PromptGroupIO
import tri.ai.prompt.PromptLibrary

class LocalMcpServerTest {

    @Test
    fun testListPrompts() {
        runTest {
            val server = LocalMcpServer(PromptLibrary.Companion.INSTANCE)
            val prompts = server.listPrompts()
            println(prompts)
            assertTrue(prompts.isNotEmpty(), "Should have at least one prompt")
            server.close()
        }
    }

    @Test
    fun testGetPrompt() {
        runTest {
            val server = LocalMcpServer(PromptLibrary.Companion.INSTANCE)

            // Test getting a prompt that exists
            val firstPrompt = server.listPrompts().first()
            val response = server.getPrompt(firstPrompt.name)
            println(response)
            Assertions.assertNotNull(response)
            assertFalse(response.messages.isEmpty())

            // Test getting a prompt that doesn't exist
            try {
                server.getPrompt("nonexistent-prompt")
                Assertions.fail("Expected McpServerException")
            } catch (e: McpServerException) {
                // Expected
            }

            server.close()
        }
    }

    @Test
    fun testGetPrompt_arguments() {
        runTest {
            val server = LocalMcpServer(PromptLibrary.Companion.INSTANCE)

            val response = server.getPrompt(
                "text-qa/answer", mapOf(
                    "instruct" to "What is the meaning of life?",
                    "input" to "42"
                )
            )
            println(response)
            Assertions.assertEquals("Question Answer Example", response.description)
            Assertions.assertEquals(1, response.messages.size)
            Assertions.assertEquals(1, response.messages[0].content!!.size)
            Assertions.assertEquals(
                listOf(
                    MChatMessagePart(
                        partType = MPartType.TEXT,
                        text = """
                Answer the following question about the text below. If you do not know the answer, say you don't know.
                ```
                42
                ```
                Question: What is the meaning of life?
                Answer:
                
            """.trimIndent()
                    )
                ), response.messages[0].content
            )

            server.close()
        }
    }

    @Test
    fun testCapabilities() {
        runTest {
            val adapter = LocalMcpServer(PromptLibrary.Companion.INSTANCE)

            val capabilities = adapter.getCapabilities()
            println(capabilities)
            Assertions.assertNotNull(capabilities)
            Assertions.assertNotNull(capabilities.prompts)

            adapter.close()
        }
    }

    val TEST_PROMPT = PromptDef(
        id = "test/prompt-id@1.00",
        category = "category",
        name = "test-prompt",
        title = "Test Prompt",
        description = "This is a test prompt template.",
        args = listOf(PromptArgDef("color", "Provide a color", true)),
        template = "Turn the color {{color}} into a hex code."
    )

    @Test
    fun testToMcpContract() {
        val mcp = TEST_PROMPT.toMcpContract()
        println(PromptGroupIO.MAPPER.writeValueAsString(mcp))
        val expected = McpPrompt(
            name = "test/prompt-id",
            title = "Test Prompt",
            description = "This is a test prompt template.",
            arguments = listOf(McpPromptArg("color", "Provide a color", true))
        )
        assertEquals(expected, mcp)
    }

}
