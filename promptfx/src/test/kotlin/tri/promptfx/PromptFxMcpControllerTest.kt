/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.mcp.McpServerRegistry

class PromptFxMcpControllerTest {

    @Test
    fun testControllerLoadsRegistry() {
        val controller = PromptFxMcpController()
        
        assertNotNull(controller.mcpServerRegistry)
        assertTrue(controller.mcpServerRegistry is McpServerRegistry)
    }

    @Test
    fun testRegistryHasExpectedServers() {
        val controller = PromptFxMcpController()
        val registry = controller.mcpServerRegistry
        
        val serverNames = registry.listServerNames()
        
        // Should have at least the default servers
        assertTrue(serverNames.contains("embedded"))
        assertTrue(serverNames.contains("test"))
    }

    @Test
    fun testRegistryCanGetServers() {
        runTest {
            val controller = PromptFxMcpController()
            val registry = controller.mcpServerRegistry
            
            // Should be able to get the embedded server
            val embeddedServer = registry.getServer("embedded")
            assertNotNull(embeddedServer)
            
            // Should be able to get the test server
            val testServer = registry.getServer("test")
            assertNotNull(testServer)
            
            // Clean up
            embeddedServer?.close()
            testServer?.close()
        }
    }

    @Test
    fun testRegistryReturnsNullForNonExistentServer() {
        val controller = PromptFxMcpController()
        val registry = controller.mcpServerRegistry
        
        val nonExistentServer = registry.getServer("nonexistent")
        assertNull(nonExistentServer)
    }
}
