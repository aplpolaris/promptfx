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

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.mcp.tool.StarterToolLibrary
import tri.ai.prompt.PromptLibrary

/**
 * Integration test that uses McpServerHttp to stand up an MCP server,
 * and McpServerAdapterHttp to be a client for that server.
 * 
 * This verifies the functionality on both the HTTP server side and HTTP client side
 * using the MCP specification for HTTP transport with JSON-RPC 2.0 on the `/mcp` endpoint.
 */
class McpHttpClientServerIntegrationTest {
    
    private val testPort = 9879

    /**
     * Integration test showing McpServerHttp and McpServerAdapterHttp working together
     * using JSON-RPC 2.0 protocol on the /mcp endpoint.
     */
    @Test
    fun testMcpServerHttpWithAdapterClient() {
        runTest {
            // Start McpServerHttp with embedded adapter
            val embeddedAdapter = McpServerEmbedded(
                PromptLibrary.INSTANCE,
                StarterToolLibrary()
            )
            val mcpServer = McpServerHttp(embeddedAdapter, testPort)
            mcpServer.startServer()
            
            delay(1000) // Wait for server to start
            
            try {
                // Verify the server is running with embedded adapter
                val serverPrompts = embeddedAdapter.listPrompts()
                assertTrue(serverPrompts.isNotEmpty(), "Server should have prompts via embedded adapter")
                
                val serverTools = embeddedAdapter.listTools()
                assertTrue(serverTools.isNotEmpty(), "Server should have tools via embedded adapter")
                
                // Create McpServerAdapterHttp client pointing to the McpServerHttp
                val httpAdapter = McpServerAdapterHttp("http://localhost:$testPort")
                
                try {
                    // Test capabilities via initialize
                    val capabilities = httpAdapter.getCapabilities()
                    assertNotNull(capabilities, "Should retrieve capabilities")
                    
                    // Test list prompts
                    val prompts = httpAdapter.listPrompts()
                    assertNotNull(prompts, "Should retrieve prompts")
                    assertTrue(prompts.isNotEmpty(), "Should have at least one prompt")
                    
                    // Test list tools
                    val tools = httpAdapter.listTools()
                    assertNotNull(tools, "Should retrieve tools")
                    assertTrue(tools.isNotEmpty(), "Should have at least one tool")
                    
                    // Test get a specific prompt
                    if (prompts.isNotEmpty()) {
                        val promptResponse = httpAdapter.getPrompt(prompts[0].name, emptyMap())
                        assertNotNull(promptResponse, "Should retrieve prompt response")
                        assertNotNull(promptResponse.messages, "Should have messages")
                    }
                    
                    // Test call a tool
                    if (tools.isNotEmpty()) {
                        val toolResult = httpAdapter.callTool(tools[0].name, emptyMap())
                        assertNotNull(toolResult, "Should get tool result")
                    }
                } finally {
                    httpAdapter.close()
                }
            } finally {
                mcpServer.close()
            }
        }
    }
}
