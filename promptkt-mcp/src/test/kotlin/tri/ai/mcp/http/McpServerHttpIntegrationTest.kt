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
package tri.ai.mcp.http

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tri.ai.mcp.McpProviderEmbedded
import tri.ai.mcp.McpProviderHttp
import tri.ai.mcp.tool.McpToolLibraryStarter
import tri.ai.prompt.PromptLibrary

/**
 * Integration test that uses [McpServerHttp] to stand up an MCP server,
 * and [tri.ai.mcp.McpProviderHttp] to be a client for that server.
 *
 * This verifies the functionality on both the HTTP server side and HTTP client side
 * using the MCP specification for HTTP transport with JSON-RPC 2.0 on the `/mcp` endpoint.
 */
class McpServerHttpIntegrationTest {

    private val testPort = 9879

    @Test
    fun `test McpServerHttp with McpProviderHttp over HTTP`() {
        runTest {
            // Start McpServerHttp with embedded provider
            val embeddedProvider = McpProviderEmbedded(PromptLibrary.INSTANCE, McpToolLibraryStarter())
            val mcpServer = McpServerHttp(embeddedProvider, testPort)
            mcpServer.startServer()

            delay(1000) // Wait for server to start

            try {
                // Verify the server is running with embedded provider
                val serverPrompts = embeddedProvider.listPrompts()
                assertTrue(serverPrompts.isNotEmpty(), "Server should have prompts via embedded provider")

                val serverTools = embeddedProvider.listTools()
                assertTrue(serverTools.isNotEmpty(), "Server should have tools via embedded provider")

                // Create provider pointing to the McpServerHttp
                val httpProvider = McpProviderHttp("http://localhost:$testPort")

                try {
                    // Test capabilities via initialize
                    val capabilities = httpProvider.getCapabilities()
                    Assertions.assertNotNull(capabilities, "Should retrieve capabilities")

                    // Test list prompts
                    val prompts = httpProvider.listPrompts()
                    Assertions.assertNotNull(prompts, "Should retrieve prompts")
                    assertTrue(prompts.isNotEmpty(), "Should have at least one prompt")

                    // Test list tools
                    val tools = httpProvider.listTools()
                    Assertions.assertNotNull(tools, "Should retrieve tools")
                    assertTrue(tools.isNotEmpty(), "Should have at least one tool")

                    // Test get a specific prompt
                    if (prompts.isNotEmpty()) {
                        val promptResponse = httpProvider.getPrompt(prompts[0].name, emptyMap())
                        Assertions.assertNotNull(promptResponse, "Should retrieve prompt response")
                        Assertions.assertNotNull(promptResponse.messages, "Should have messages")
                    }

                    // Test call a tool
                    if (tools.isNotEmpty()) {
                        val toolResult = httpProvider.callTool(tools[0].name, emptyMap())
                        Assertions.assertNotNull(toolResult, "Should get tool result")
                    }
                } finally {
                    httpProvider.close()
                }
            } finally {
                mcpServer.close()
            }
        }
    }
}
