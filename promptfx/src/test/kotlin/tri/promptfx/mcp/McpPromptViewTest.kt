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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tri.promptfx.PromptFxMcpController

class McpPromptViewTest {

    @Test
    fun testMcpControllerLoadsServers() {
        val controller = PromptFxMcpController()
        val serverNames = controller.mcpServerRegistry.listServerNames()
        
        assertTrue(serverNames.isNotEmpty(), "MCP server registry should have at least one server")
        assertTrue(serverNames.contains("embedded") || serverNames.contains("test"), 
            "MCP server registry should contain 'embedded' or 'test' server")
    }

    @Test
    fun testMcpServersProvidePrompts() = runBlocking {
        val controller = PromptFxMcpController()
        val serverNames = controller.mcpServerRegistry.listServerNames()
        
        var totalPrompts = 0
        for (serverName in serverNames) {
            val server = controller.mcpServerRegistry.getServer(serverName)
            if (server != null) {
                val prompts = server.listPrompts()
                totalPrompts += prompts.size
            }
        }
        
        assertTrue(totalPrompts > 0, "At least one MCP server should provide prompts")
    }

    @Test
    fun testMcpPromptWithServerCreation() = runBlocking {
        val controller = PromptFxMcpController()
        val serverName = "test"
        val server = controller.mcpServerRegistry.getServer(serverName)
        
        if (server != null) {
            val prompts = server.listPrompts()
            if (prompts.isNotEmpty()) {
                val prompt = prompts.first()
                val promptWithServer = McpPromptWithServer(prompt, serverName)
                
                assertTrue(promptWithServer.prompt.name.isNotEmpty(), "Prompt should have a name")
                assertTrue(promptWithServer.serverName == serverName, "Server name should match")
            }
        }
    }
}
