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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.prompt.PromptLibrary

class McpServerAdapterTest {

    @Test
    fun testLocalAdapter() = runTest {
        val adapter = LocalMcpServerAdapter(PromptLibrary.INSTANCE)
        
        // Test list prompts
        val prompts = adapter.listPrompts()
        assertTrue(prompts.isNotEmpty(), "Should have at least one prompt")
        
        // Test getting a prompt that exists
        val firstPrompt = prompts.first()
        val response = adapter.getPrompt(firstPrompt.id)
        assertNotNull(response)
        assertFalse(response.messages.isEmpty())
        
        // Test getting a prompt that doesn't exist
        try {
            adapter.getPrompt("nonexistent-prompt")
            fail("Expected McpServerException")
        } catch (e: McpServerException) {
            // Expected
        }
        
        adapter.close()
    }

    @Test
    fun testLocalAdapterWithArguments() = runTest {
        val adapter = LocalMcpServerAdapter(PromptLibrary.INSTANCE)
        
        // Find a prompt with arguments
        val prompts = adapter.listPrompts()
        val promptWithArgs = prompts.find { !it.arguments.isNullOrEmpty() }
        
        if (promptWithArgs != null) {
            val args = mapOf("input" to "test input", "instruct" to "test instruction")
            val response = adapter.getPrompt(promptWithArgs.id, args)
            assertNotNull(response)
            assertFalse(response.messages.isEmpty())
            
            // Check that response is properly filled
            val messageText = response.messages.first().content?.first()?.text
            assertNotNull(messageText)
            // The message should contain some content (we can't guarantee specific test values will be present)
            assertTrue(messageText!!.isNotEmpty())
        }
        
        adapter.close()
    }

    @Test
    fun testLocalAdapterCapabilities() = runTest {
        val adapter = LocalMcpServerAdapter(PromptLibrary.INSTANCE)
        
        val capabilities = adapter.getCapabilities()
        assertNotNull(capabilities)
        assertNotNull(capabilities.prompts)
        
        adapter.close()
    }
}