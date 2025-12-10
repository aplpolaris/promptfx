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
 * This verifies the functionality on both the HTTP server side and HTTP client side.
 * 
 * **Note**: McpServerHttp uses JSON-RPC 2.0 protocol (POST to `/`), while McpServerAdapterHttp
 * expects REST-style endpoints (GET/POST to `/capabilities`, `/prompts/list`, etc.).
 * These components use different protocols and cannot communicate with each other directly.
 * This test demonstrates this architectural limitation.
 */
class McpHttpClientServerIntegrationTest {
    
    private val testPort = 9879

    /**
     * Integration test showing that McpServerHttp and McpServerAdapterHttp 
     * use incompatible protocols and cannot communicate with each other.
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
                    // Attempt to communicate - this will fail because of protocol mismatch
                    // McpServerHttp uses JSON-RPC (POST to /)
                    // McpServerAdapterHttp expects REST (GET /capabilities, etc.)
                    
                    // Test capabilities - will return null because /capabilities endpoint doesn't exist
                    val capabilities = httpAdapter.getCapabilities()
                    assertNull(capabilities, "Capabilities should be null - REST endpoint /capabilities not found on JSON-RPC server")
                    
                    // Attempting to list prompts will fail because /prompts/list doesn't exist
                    // McpServerAdapterHttp expects GET /prompts/list but McpServerHttp only has POST /
                    try {
                        httpAdapter.listPrompts()
                        fail("Should not be able to list prompts - protocol mismatch")
                    } catch (e: Exception) {
                        // Expected - protocols are incompatible
                        assertTrue(e is McpServerException || e.cause is McpServerException,
                            "Should fail with protocol mismatch")
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
