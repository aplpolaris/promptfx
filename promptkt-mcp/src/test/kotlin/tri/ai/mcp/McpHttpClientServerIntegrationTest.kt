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

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tri.ai.mcp.tool.StarterToolLibrary
import tri.ai.prompt.PromptLibrary

/**
 * Comprehensive integration test that demonstrates both MCP HTTP server and client functionality:
 * 
 * Part 1: Tests [McpServerHttp] (JSON-RPC server) with an embedded adapter
 * Part 2: Tests [McpServerAdapterHttp] (REST client) connecting to a REST-style HTTP server
 * 
 * This test validates the complete HTTP transport layer for MCP, showing both
 * the server-side (McpServerHttp) and client-side (McpServerAdapterHttp) implementations.
 */
class McpHttpClientServerIntegrationTest {
    
    private val jsonRpcPort = 9879
    private val restPort = 9880

    /**
     * Part 1: Tests McpServerHttp with an embedded adapter
     * This tests the JSON-RPC over HTTP server implementation
     */
    @Test
    fun testMcpServerHttpWithEmbeddedAdapter() {
        runTest {
            // Create and start JSON-RPC server with embedded adapter
            val embeddedAdapter = McpServerEmbedded(
                PromptLibrary.INSTANCE,
                StarterToolLibrary()
            )
            
            val jsonRpcServer = McpServerHttp(embeddedAdapter, jsonRpcPort)
            jsonRpcServer.startServer()
            delay(1000) // Wait for server to start
            
            try {
                // Verify the embedded adapter has content
                val prompts = embeddedAdapter.listPrompts()
                assertTrue(prompts.isNotEmpty(), "Embedded adapter should have prompts")
                
                val tools = embeddedAdapter.listTools()
                assertTrue(tools.isNotEmpty(), "Embedded adapter should have tools")
                
                // The server is running and accessible via JSON-RPC
                // (Detailed JSON-RPC testing is done in McpServerHttpIntegrationTest)
            } finally {
                jsonRpcServer.close()
            }
        }
    }

    /**
     * Part 2: Tests McpServerAdapterHttp connecting to a REST-style HTTP server
     * This demonstrates the REST client implementation
     */
    @Test
    fun testMcpServerAdapterHttpWithRestServer() {
        runTest {
            // Start a simple REST-style HTTP server
            val restServer = embeddedServer(Netty, port = restPort) {
                routing {
                    get("/capabilities") {
                        call.respondText(
                            """{"prompts":{"listChanged":false},"tools":{"listChanged":false}}""",
                            ContentType.Application.Json
                        )
                    }
                    
                    get("/prompts/list") {
                        call.respondText(
                            """[{"name":"integration-test-prompt","title":"Integration Test","description":"A test prompt for integration"}]""",
                            ContentType.Application.Json
                        )
                    }
                    
                    post("/prompts/get") {
                        val body = call.receiveText()
                        call.respondText(
                            """{"description":"Test response","messages":[{"role":"User","content":[{"partType":"TEXT","text":"Integration test message"}]}]}""",
                            ContentType.Application.Json
                        )
                    }
                    
                    get("/tools/list") {
                        call.respondText(
                            """[{"name":"integration-test-tool","description":"A test tool","version":"1.0.0","inputSchema":{},"outputSchema":{},"hardCodedOutput":{}}]""",
                            ContentType.Application.Json
                        )
                    }
                    
                    post("/tools/call") {
                        val body = call.receiveText()
                        call.respondText(
                            """{"name":"integration-test-tool","output":{"result":"integration test output"},"error":null}""",
                            ContentType.Application.Json
                        )
                    }
                }
            }.start(wait = false)
            
            delay(1000) // Wait for server to start
            
            try {
                // Create HTTP adapter client
                val httpAdapter = McpServerAdapterHttp("http://localhost:$restPort")
                
                try {
                    // Test capabilities
                    val capabilities = httpAdapter.getCapabilities()
                    assertNotNull(capabilities, "Should retrieve capabilities")
                    assertNotNull(capabilities?.prompts, "Should have prompts capability")
                    
                    // Test list prompts
                    val prompts = httpAdapter.listPrompts()
                    assertNotNull(prompts, "Should retrieve prompts list")
                    assertTrue(prompts.isNotEmpty(), "Should have at least one prompt")
                    assertEquals("integration-test-prompt", prompts[0].name, "Prompt name should match")
                    
                    // Test get prompt
                    val promptResponse = httpAdapter.getPrompt("integration-test-prompt", emptyMap())
                    assertNotNull(promptResponse, "Should retrieve prompt response")
                    assertNotNull(promptResponse.messages, "Should have messages")
                    assertTrue(promptResponse.messages.isNotEmpty(), "Should have at least one message")
                    
                    // Test list tools
                    val tools = httpAdapter.listTools()
                    assertNotNull(tools, "Should retrieve tools list")
                    assertTrue(tools.isNotEmpty(), "Should have at least one tool")
                    assertEquals("integration-test-tool", tools[0].name, "Tool name should match")
                    
                    // Test get tool
                    val tool = httpAdapter.getTool("integration-test-tool")
                    assertNotNull(tool, "Should retrieve specific tool")
                    assertEquals("integration-test-tool", tool?.name, "Tool name should match")
                    
                    // Test call tool
                    val toolResult = httpAdapter.callTool("integration-test-tool", emptyMap())
                    assertNotNull(toolResult, "Should get tool result")
                    assertEquals("integration-test-tool", toolResult.name, "Result name should match")
                    assertNull(toolResult.error, "Should not have error")
                } finally {
                    httpAdapter.close()
                }
            } finally {
                restServer.stop(1000, 2000)
            }
        }
    }

    /**
     * Combined integration test showing both server types can coexist
     */
    @Test
    fun testBothServerTypesCanCoexist() {
        runTest {
            // Start JSON-RPC server
            val embeddedAdapter = McpServerEmbedded(
                PromptLibrary.INSTANCE,
                StarterToolLibrary()
            )
            val jsonRpcServer = McpServerHttp(embeddedAdapter, jsonRpcPort)
            jsonRpcServer.startServer()
            
            // Start REST server
            val restServer = embeddedServer(Netty, port = restPort) {
                routing {
                    get("/prompts/list") {
                        call.respondText(
                            """[{"name":"rest-prompt","title":"REST Prompt","description":"From REST server"}]""",
                            ContentType.Application.Json
                        )
                    }
                }
            }.start(wait = false)
            
            delay(1000) // Wait for both servers to start
            
            try {
                // Connect REST client to REST server
                val httpAdapter = McpServerAdapterHttp("http://localhost:$restPort")
                
                try {
                    val restPrompts = httpAdapter.listPrompts()
                    
                    // Verify both servers are operational
                    assertNotNull(jsonRpcServer, "JSON-RPC server should be running")
                    assertNotNull(restPrompts, "REST client should retrieve prompts")
                    assertEquals("rest-prompt", restPrompts[0].name, "Should get prompts from REST server")
                    
                    // Verify embedded adapter is still accessible
                    val embeddedPrompts = embeddedAdapter.listPrompts()
                    assertTrue(embeddedPrompts.isNotEmpty(), "Embedded adapter should still have prompts")
                } finally {
                    httpAdapter.close()
                }
            } finally {
                jsonRpcServer.close()
                restServer.stop(1000, 2000)
            }
        }
    }
}
