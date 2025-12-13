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
package tri.promptfx.mcp

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.promptfx.PromptFxMcpController

class McpServerViewTest {

    @Test
    fun `test controller loads servers from registry`() {
        val controller = PromptFxMcpController()
        val serverNames = controller.mcpServerRegistry.listServerNames()
        
        // Should have at least the default servers
        assertTrue(serverNames.isNotEmpty(), "Should have at least one server")
        assertTrue(serverNames.contains("embedded"), "Should contain embedded server")
        assertTrue(serverNames.contains("test"), "Should contain test server")
    }
    
    @Test
    fun `test server configs are accessible`() {
        val controller = PromptFxMcpController()
        val configs = controller.mcpServerRegistry.getConfigs()
        
        // Should be able to access configs
        assertNotNull(configs["embedded"], "Should have embedded server config")
        assertNotNull(configs["test"], "Should have test server config")
        
        // Verify configs have descriptions
        val embeddedConfig = configs["embedded"]
        assertNotNull(embeddedConfig?.description, "Embedded server should have a description")
        
        val testConfig = configs["test"]
        assertNotNull(testConfig?.description, "Test server should have a description")
    }
}
