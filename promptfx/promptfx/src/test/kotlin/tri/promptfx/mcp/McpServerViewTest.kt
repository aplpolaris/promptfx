/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx.mcp

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.promptfx.PromptFxMcpController

class McpServerViewTest {

    @Test
    fun `test controller loads servers from registry`() {
        val controller = PromptFxMcpController()
        val serverNames = controller.mcpProviderRegistry.listProviderNames()
        
        // Should have at least the default servers
        assertTrue(serverNames.isNotEmpty(), "Should have at least one server")
        assertTrue(serverNames.contains("embedded"), "Should contain embedded server")
        assertTrue(serverNames.contains("test"), "Should contain test server")
    }
    
    @Test
    fun `test server configs are accessible`() {
        val controller = PromptFxMcpController()
        val configs = controller.mcpProviderRegistry.getConfigs()
        
        // Should be able to access configs
        assertNotNull(configs["embedded"], "Should have embedded server config")
        assertNotNull(configs["test"], "Should have test server config")
        
        // Verify configs have descriptions
        val embeddedConfig = configs["embedded"]
        assertNotNull(embeddedConfig?.description, "Embedded server should have a description")
        
        val testConfig = configs["test"]
        assertNotNull(testConfig?.description, "Test server should have a description")
    }
    
    @Test
    fun `test server capabilities can be queried`() = runBlocking {
        val controller = PromptFxMcpController()
        
        // Test the embedded server
        val embeddedServer = controller.mcpProviderRegistry.getProvider("embedded")
        assertNotNull(embeddedServer, "Should be able to get embedded server")
        
        val capabilities = embeddedServer?.getCapabilities()
        assertNotNull(capabilities, "Should have capabilities")
        
        // Embedded server should have prompts capability
        assertNotNull(capabilities?.prompts, "Embedded server should support prompts")
    }
    
    @Test
    fun `test server capability info data class`() {
        // Test that ServerCapabilityInfo can be created and used
        val info1 = ProviderCapabilityInfo(
            hasPrompts = true,
            hasTools = true,
            hasResources = false,
            promptsCount = 5,
            toolsCount = 3,
            resourcesCount = 0,
            resourceTemplatesCount = 0
        )
        
        assertTrue(info1.hasPrompts)
        assertTrue(info1.hasTools)
        assertFalse(info1.hasResources)
        assertEquals(5, info1.promptsCount)
        assertEquals(3, info1.toolsCount)
        assertEquals(0, info1.resourceTemplatesCount)
        assertNull(info1.error)
        assertFalse(info1.loading)
        
        // Test error state
        val info2 = ProviderCapabilityInfo(error = "Connection failed")
        assertNotNull(info2.error)
        assertEquals("Connection failed", info2.error)
        assertFalse(info2.loading)
        
        // Test loading state
        val info3 = ProviderCapabilityInfo(loading = true)
        assertTrue(info3.loading)
        assertFalse(info3.hasPrompts)
        assertFalse(info3.hasTools)
        assertFalse(info3.hasResources)
        assertNull(info3.error)
    }
}
