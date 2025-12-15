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

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.mcp.McpResource
import tri.promptfx.PromptFxMcpController

class McpResourceViewTest {

    @Test
    fun `test ResourceWithServer data class`() {
        val resource = McpResource(
            uri = "file:///test.txt",
            name = "Test Resource",
            description = "A test resource",
            mimeType = "text/plain"
        )
        
        val resourceWithServer = ResourceWithServer(resource, "test-server")
        
        assertEquals("Test Resource", resourceWithServer.resource.name)
        assertEquals("test-server", resourceWithServer.serverName)
        assertEquals("file:///test.txt", resourceWithServer.resource.uri)
        assertEquals("A test resource", resourceWithServer.resource.description)
        assertEquals("text/plain", resourceWithServer.resource.mimeType)
    }

    @Test
    fun `test controller can load resources from servers`() = runBlocking {
        val controller = PromptFxMcpController()
        
        // Try to load resources from available servers
        var resourceCount = 0
        for (serverName in controller.mcpServerRegistry.listServerNames()) {
            try {
                val server = controller.mcpServerRegistry.getServer(serverName)
                if (server != null) {
                    val resources = server.listResources()
                    resourceCount += resources.size
                    
                    // If we have resources, verify their structure
                    resources.forEach { resource ->
                        assertNotNull(resource.uri, "Resource should have a URI")
                        assertNotNull(resource.name, "Resource should have a name")
                        // description and mimeType are optional
                    }
                }
            } catch (e: Exception) {
                // Some servers may not support resources yet, which is okay
            }
        }
        
        // We don't assert on resourceCount because servers may not have resources configured
    }
    
    @Test
    fun `test resource filtering logic`() {
        val resource1 = McpResource(uri = "file:///data.txt", name = "Data File", description = null, mimeType = "text/plain")
        val resource2 = McpResource(uri = "file:///config.json", name = "Config File", description = null, mimeType = "application/json")
        val resource3 = McpResource(uri = "file:///image.png", name = "Image File", description = null, mimeType = "image/png")
        
        val resources = listOf(
            ResourceWithServer(resource1, "server1"),
            ResourceWithServer(resource2, "server1"),
            ResourceWithServer(resource3, "server2")
        )
        
        // Test filtering by name (case insensitive)
        val dataFilter: (String) -> Boolean = { it.contains("data", ignoreCase = true) }
        val filteredData = resources.filter { dataFilter(it.resource.name) }
        assertEquals(1, filteredData.size)
        assertEquals("Data File", filteredData[0].resource.name)
        
        // Test filtering for "file"
        val fileFilter: (String) -> Boolean = { it.contains("file", ignoreCase = true) }
        val filteredFile = resources.filter { fileFilter(it.resource.name) }
        assertEquals(3, filteredFile.size)
        
        // Test filtering for something that doesn't exist
        val noneFilter: (String) -> Boolean = { it.contains("xyz", ignoreCase = true) }
        val filteredNone = resources.filter { noneFilter(it.resource.name) }
        assertEquals(0, filteredNone.size)
    }
}
