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
package tri.promptfx.settings

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PromptFxSettingsViewTest {

    @Test
    fun testConfigCategoryContainsMcpServers() {
        val categories = ConfigCategory.entries.toTypedArray()
        
        // Verify that MCP_SERVERS category exists
        assertTrue(categories.contains(ConfigCategory.MCP_SERVERS), 
            "ConfigCategory should contain MCP_SERVERS")
    }

    @Test
    fun testMcpServersCategoryProperties() {
        val mcpServers = ConfigCategory.MCP_SERVERS
        
        assertEquals("MCP Servers", mcpServers.displayName, 
            "Display name should be 'MCP Servers'")
        assertEquals("MCP Servers", mcpServers.title, 
            "Title should be 'MCP Servers'")
        assertEquals("Model Context Protocol server configurations.", mcpServers.description,
            "Description should match expected value")
    }

    @Test
    fun testConfigCategoryOrder() {
        val categories = ConfigCategory.entries.toTypedArray()
        
        // Verify that MCP_SERVERS comes after RUNTIME and before CONFIG_FILES
        val runtimeIndex = categories.indexOf(ConfigCategory.RUNTIME)
        val mcpServersIndex = categories.indexOf(ConfigCategory.MCP_SERVERS)
        val configFilesIndex = categories.indexOf(ConfigCategory.CONFIG_FILES)
        
        assertTrue(mcpServersIndex > runtimeIndex, 
            "MCP_SERVERS should come after RUNTIME")
        assertTrue(mcpServersIndex < configFilesIndex, 
            "MCP_SERVERS should come before CONFIG_FILES")
    }

    @Test
    fun testAllCategoriesAreUnique() {
        val categories = ConfigCategory.entries.toTypedArray()
        val displayNames = categories.map { it.displayName }
        
        // Verify all display names are unique
        assertEquals(displayNames.size, displayNames.toSet().size,
            "All category display names should be unique")
    }
}
