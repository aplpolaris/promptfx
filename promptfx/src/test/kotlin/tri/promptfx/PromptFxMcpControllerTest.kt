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
import org.junit.jupiter.api.io.TempDir
import tri.ai.mcp.McpProviderRegistry
import java.nio.file.Path

class PromptFxMcpControllerTest {

    @Test
    fun testControllerLoadsRegistry() {
        val controller = PromptFxMcpController()
        
        assertNotNull(controller.mcpProviderRegistry)
        assertTrue(controller.mcpProviderRegistry is McpProviderRegistry)
    }

    @Test
    fun testRegistryHasExpectedServers() {
        val controller = PromptFxMcpController()
        val registry = controller.mcpProviderRegistry
        
        val serverNames = registry.listProviderNames()
        
        // Should have at least the default servers
        assertTrue(serverNames.contains("embedded"))
        assertTrue(serverNames.contains("test"))
    }

    @Test
    fun testRegistryCanGetServers() {
        runTest {
            val controller = PromptFxMcpController()
            val registry = controller.mcpProviderRegistry
            
            // Should be able to get the embedded server
            val embeddedServer = registry.getProvider("embedded")
            assertNotNull(embeddedServer)
            
            // Should be able to get the test server
            val testServer = registry.getProvider("test")
            assertNotNull(testServer)
            
            // Clean up
            embeddedServer?.close()
            testServer?.close()
        }
    }

    @Test
    fun testRegistryReturnsNullForNonExistentServer() {
        val controller = PromptFxMcpController()
        val registry = controller.mcpProviderRegistry
        
        val nonExistentServer = registry.getProvider("nonexistent")
        assertNull(nonExistentServer)
    }

    @Test
    fun testRuntimeConfigFileLoading(@TempDir tempDir: Path) {
        // Create a config directory
        val configDir = tempDir.resolve("config").toFile()
        configDir.mkdirs()
        
        // Create a test mcp-servers.yaml file in config directory
        val configFile = configDir.resolve("mcp-servers.yaml")
        configFile.writeText("""
            servers:
              runtime-test:
                type: test
                description: "Runtime test server"
                includeDefaultPrompts: false
                includeDefaultTools: false
        """.trimIndent())
        
        // Load registry directly from the file
        val registry = McpProviderRegistry.loadFromYaml(configFile)
        
        // Should have the runtime-test server
        val serverNames = registry.listProviderNames()
        assertTrue(serverNames.contains("runtime-test"), "Should contain runtime-test server from config/mcp-servers.yaml")
        
        // Clean up
        configFile.delete()
    }
}
