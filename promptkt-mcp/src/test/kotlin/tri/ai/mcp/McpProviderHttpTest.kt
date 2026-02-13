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

import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tri.ai.mcp.McpJsonRpcHandler.Companion.METHOD_INITIALIZE
import tri.ai.mcp.McpJsonRpcHandler.Companion.METHOD_NOTIFICATIONS_INITIALIZED
import tri.ai.mcp.McpJsonRpcHandler.Companion.METHOD_PROMPTS_GET
import tri.ai.mcp.McpJsonRpcHandler.Companion.METHOD_PROMPTS_LIST
import tri.ai.mcp.McpJsonRpcHandler.Companion.METHOD_TOOLS_CALL
import tri.ai.mcp.McpJsonRpcHandler.Companion.METHOD_TOOLS_LIST
import java.util.concurrent.atomic.AtomicReference

class McpProviderHttpTest {

    companion object {
        private const val TEST_PORT = 9876
        private const val BASE_URL = "http://localhost:$TEST_PORT"
        private lateinit var server: NettyApplicationEngine
        private val lastReceivedSessionId = AtomicReference<String?>()

        @JvmStatic
        @BeforeAll
        fun startServer() {
            server = embeddedServer(Netty, port = TEST_PORT) {
                routing {
                    post("/mcp") {
                        val body = call.receiveText()
                        val json = Json.parseToJsonElement(body).jsonObject
                        val method = json["method"]?.jsonPrimitive?.content
                        val id = json["id"]
                        
                        // Track received session ID for verification
                        val receivedSessionId = call.request.headers[McpProviderHttp.HEADER_MCP_SESSION_ID]
                        lastReceivedSessionId.set(receivedSessionId)
                        
                        val response = when (method) {
                            METHOD_INITIALIZE -> """{"jsonrpc":"2.0","id":$id,"result":{"protocolVersion":"2025-06-18","serverInfo":{"name":"test-server","version":"1.0.0"},"capabilities":{"prompts":{"listChanged":false},"tools":null}}}"""
                            METHOD_NOTIFICATIONS_INITIALIZED -> """{"jsonrpc":"2.0","id":$id,"result":{}}"""
                            METHOD_PROMPTS_LIST -> """{"jsonrpc":"2.0","id":$id,"result":{"prompts":[{"name":"test-prompt","title":"Test Prompt","description":"A test prompt"}]}}"""
                            METHOD_PROMPTS_GET -> """{"jsonrpc":"2.0","id":$id,"result":{"description":"Test prompt response","messages":[{"role":"user","content":{"type":"text","text":"Test message"}}]}}"""
                            METHOD_TOOLS_LIST -> """{"jsonrpc":"2.0","id":$id,"result":{"tools":[{"name":"test-tool","description":"A test tool","version":"1.0.0","inputSchema":{},"outputSchema":{}}]}}"""
                            METHOD_TOOLS_CALL -> """{"jsonrpc":"2.0","id":$id,"result":{"content":[{"type":"text","text":"test output"}],"structuredContent":{"result":"test output"}}}"""
                            else -> """{"jsonrpc":"2.0","id":$id,"error":{"code":-32601,"message":"Method not found"}}"""
                        }
                        
                        // Send session ID in response for initialize method
                        if (method == METHOD_INITIALIZE) {
                            call.response.header(McpProviderHttp.HEADER_MCP_SESSION_ID, "test-session-123")
                        }
                        
                        call.respondText(response, ContentType.Application.Json)
                    }
                }
            }.start(wait = false)

            Thread.sleep(1000) // Wait for server to start
        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            server.stop(1000, 2000)
        }
    }

    @Test
    fun testGetCapabilities() {
        runTest {
            val provider = McpProviderHttp(BASE_URL)

            val capabilities = provider.getCapabilities()
            println("testGetCapabilities result: $capabilities")
            assertNotNull(capabilities)
            assertNotNull(capabilities?.prompts)
            assertEquals(false, capabilities?.prompts?.listChanged)

            provider.close()
        }
    }

    @Test
    fun testListPrompts() {
        runTest {
            val provider = McpProviderHttp(BASE_URL)

            try {
                val prompts = provider.listPrompts()
                println("testListPrompts result: $prompts")
                assertNotNull(prompts)
                assertTrue(prompts.size == 1, "Should have at least one prompt")
                assertEquals("test-prompt", prompts[0].name)
                assertEquals("Test Prompt", prompts[0].title)
            } finally {
                provider.close()
            }
        }
    }

    @Test
    fun testGetPrompt() {
        runTest {
            val provider = McpProviderHttp(BASE_URL)

            try {
                val response = provider.getPrompt("test-prompt", mapOf("arg1" to "value1"))
                println("testGetPrompt result: $response")
                assertNotNull(response)
                assertEquals("Test prompt response", response.description)
                assertTrue(response.messages.size == 1, "Should have at least one message")
            } finally {
                provider.close()
            }
        }
    }

    @Test
    fun testListTools() {
        runTest {
            val provider = McpProviderHttp(BASE_URL)

            try {
                val tools = provider.listTools()
                println("testListTools result: $tools")
                assertNotNull(tools)
                assertTrue(tools.size == 1, "Should have at least one tool")
                assertEquals("test-tool", tools[0].name)
            } finally {
                provider.close()
            }
        }
    }

    @Test
    fun testGetTool() {
        runTest {
            val provider = McpProviderHttp(BASE_URL)

            try {
                val tool = provider.getTool("test-tool")
                println("testGetTool result: $tool")
                assertNotNull(tool, "Tool should be found")
                assertEquals("test-tool", tool?.name)
            } finally {
                provider.close()
            }
        }
    }

    @Test
    fun testCallTool() {
        runTest {
            val provider = McpProviderHttp(BASE_URL)
            try {
                val result = provider.callTool("test-tool", mapOf("input" to "test"))
                println("testCallTool result: $result")
                assertNotNull(result)
                assertNotNull(result.content)
                assertNull(result.isError)
            } finally {
                provider.close()
            }
        }
    }

    @Test
    fun testConnectionError() {
        runTest {
            val provider = McpProviderHttp("http://localhost:99999")

            try {
                try {
                    provider.listPrompts()
                    fail("Should have thrown an exception")
                } catch (e: Exception) {
                    // Expected
                }
            } finally {
                provider.close()
            }
        }
    }

    @Test
    fun testSessionIdHandling() {
        runTest {
            val provider = McpProviderHttp(BASE_URL)

            try {
                // Reset session ID tracker
                lastReceivedSessionId.set(null)
                
                // First call - initialize which should receive session ID from server
                val capabilities = provider.getCapabilities()
                assertNotNull(capabilities)
                
                // Clear the tracker before next call
                lastReceivedSessionId.set(null)
                
                // Second call - should send session ID in header
                val prompts = provider.listPrompts()
                assertNotNull(prompts)
                
                // Verify that the session ID was sent in the request header
                assertEquals("test-session-123", lastReceivedSessionId.get(), 
                    "Session ID should be sent in subsequent requests after initialization")
            } finally {
                provider.close()
            }
        }
    }

    @Test
    @Disabled("This test requires an external MCP HTTP server to be running at the specified URL")
    fun testExternal() {
        val url = "https://seolinkmap.com/mcp"
//        val url = "https://your-test-server/mcp"
        val provider = McpProviderHttp(url)
        runTest {
            println(provider.getCapabilities())
            println(provider.listTools())
            provider.close()
        }
    }

}
