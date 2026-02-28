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
package tri.ai.mcp

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tri.ai.core.MChatMessagePart
import tri.ai.core.MPartType
import tri.ai.mcp.tool.McpToolLibraryStarter
import tri.ai.prompt.PromptArgDef
import tri.ai.prompt.PromptDef
import tri.ai.prompt.PromptGroupIO
import tri.ai.prompt.PromptLibrary

class McpProviderEmbeddedTest {

    @Test
    fun testListPrompts() {
        runTest {
            val provider = McpProviderEmbedded(PromptLibrary.INSTANCE, McpToolLibraryStarter())
            val prompts = provider.listPrompts()
            println(prompts)
            assertTrue(prompts.isNotEmpty(), "Should have at least one prompt")
            provider.close()
        }
    }

    @Test
    fun testListResources() {
        runTest {
            val provider = McpProviderEmbedded(PromptLibrary.INSTANCE, McpToolLibraryStarter())
            val resources = provider.listResources()
            println(resources)
            assertTrue(resources.isEmpty(), "Default embedded provider should have no resources")
            provider.close()
        }
    }

    @Test
    fun testListResourceTemplates() {
        runTest {
            val provider = McpProviderEmbedded(PromptLibrary.INSTANCE, McpToolLibraryStarter())
            val templates = provider.listResourceTemplates()
            println(templates)
            assertTrue(templates.isEmpty(), "Default embedded provider should have no resource templates")
            provider.close()
        }
    }

    @Test
    fun testReadResource_notFound() {
        runTest {
            val provider = McpProviderEmbedded(PromptLibrary.INSTANCE, McpToolLibraryStarter())
            try {
                provider.readResource("file:///nonexistent")
                Assertions.fail("Expected McpproviderException for nonexistent resource")
            } catch (e: McpException) {
                // Expected
            }
            provider.close()
        }
    }

    @Test
    fun testGetPrompt() {
        runTest {
            val provider = McpProviderEmbedded(PromptLibrary.Companion.INSTANCE, McpToolLibraryStarter())

            // Test getting a prompt that exists
            val firstPrompt = provider.listPrompts().first()
            val response = provider.getPrompt(firstPrompt.name)
            println(response)
            Assertions.assertNotNull(response)
            assertFalse(response.messages.isEmpty())

            // Test getting a prompt that doesn't exist
            try {
                provider.getPrompt("nonexistent-prompt")
                Assertions.fail("Expected McpproviderException")
            } catch (e: McpException) {
                // Expected
            }

            provider.close()
        }
    }

    @Test
    fun testGetPrompt_arguments() {
        runTest {
            val provider = McpProviderEmbedded(PromptLibrary.Companion.INSTANCE, McpToolLibraryStarter())

            val response = provider.getPrompt(
                "text-qa/answer", mapOf(
                    "instruct" to "What is the meaning of life?",
                    "input" to "42"
                )
            )
            println(response)
            Assertions.assertEquals("Answer a question based on the provided text.", response.description)
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

            provider.close()
        }
    }

    @Test
    fun testCapabilities() {
        runTest {
            val provider = McpProviderEmbedded(PromptLibrary.INSTANCE, McpToolLibraryStarter())

            val capabilities = provider.getCapabilities()
            println(capabilities)
            Assertions.assertNotNull(capabilities)
            Assertions.assertNotNull(capabilities.prompts)

            provider.close()
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
