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

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tri.ai.core.MChatMessagePart
import tri.ai.core.MPartType
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.tool.Executable
import tri.ai.mcp.tool.McpToolResult

class McpServerHttpTest {

    private lateinit var mcpServer: McpServerHttp
    private lateinit var httpClient: HttpClient
    private val testPort = 9877

    @BeforeEach
    fun setup() {
        httpClient = HttpClient(OkHttp)
    }

    @AfterEach
    fun teardown() {
        runTest {
            if (::mcpServer.isInitialized) {
                mcpServer.close()
            }
            httpClient.close()
        }
    }

    private suspend fun waitForServerReady(maxAttempts: Int = 10) {
        repeat(maxAttempts) { attempt ->
            try {
                val response = httpClient.post("http://localhost:$testPort/") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"jsonrpc":"2.0","id":0,"method":"ping"}""")
                }
                if (response.status.isSuccess() || response.status == HttpStatusCode.OK) {
                    return
                }
            } catch (e: Exception) {
                if (attempt < maxAttempts - 1) {
                    kotlinx.coroutines.delay(100)
                } else {
                    throw Exception("Server failed to start after $maxAttempts attempts", e)
                }
            }
        }
    }

    @Test
    fun testStartServerAndInitialize() {
        runTest {
            val adapter = createTestAdapter()
            mcpServer = McpServerHttp(adapter, testPort)
            mcpServer.startServer()
            
            waitForServerReady()
            
            val response = sendJsonRpcRequest("initialize", null)
            
            assertTrue(response.contains("\"protocolVersion\""))
            assertTrue(response.contains("\"serverInfo\""))
            assertTrue(response.contains("promptfx-prompts"))
        }
    }

    @Test
    fun testListPrompts() {
        runTest {
            val adapter = createTestAdapter()
            mcpServer = McpServerHttp(adapter, testPort)
            mcpServer.startServer()
            
            waitForServerReady()
            
            val response = sendJsonRpcRequest("prompts/list", null)
            
            assertTrue(response.contains("\"prompts\""))
            assertTrue(response.contains("test-prompt"))
        }
    }

    @Test
    fun testGetPrompt() {
        runTest {
            val adapter = createTestAdapter()
            mcpServer = McpServerHttp(adapter, testPort)
            mcpServer.startServer()
            
            waitForServerReady()
            
            val params = buildJsonObject {
                put("name", "test-prompt")
                put("arguments", buildJsonObject {})
            }
            
            val response = sendJsonRpcRequest("prompts/get", params)
            
            assertTrue(response.contains("\"messages\""))
        }
    }

    @Test
    fun testListTools() {
        runTest {
            val adapter = createTestAdapter()
            mcpServer = McpServerHttp(adapter, testPort)
            mcpServer.startServer()
            
            waitForServerReady()
            
            val response = sendJsonRpcRequest("tools/list", null)
            
            assertTrue(response.contains("\"tools\""))
        }
    }

    @Test
    fun testInvalidMethod() {
        runTest {
            val adapter = createTestAdapter()
            mcpServer = McpServerHttp(adapter, testPort)
            mcpServer.startServer()
            
            waitForServerReady()
            
            val response = sendJsonRpcRequest("invalid/method", null)
            
            assertTrue(response.contains("\"error\""))
            assertTrue(response.contains("-32601") || response.contains("Method not found"))
        }
    }

    @Test
    fun testMalformedRequest() {
        runTest {
            val adapter = createTestAdapter()
            mcpServer = McpServerHttp(adapter, testPort)
            mcpServer.startServer()
            
            waitForServerReady()
            
            val response = httpClient.post("http://localhost:$testPort/") {
                contentType(ContentType.Application.Json)
                setBody("{not valid json")
            }
            
            val responseText = response.bodyAsText()
            assertTrue(responseText.contains("\"error\""))
            assertTrue(responseText.contains("-32700") || responseText.contains("Parse error"))
        }
    }

    private suspend fun sendJsonRpcRequest(method: String, params: JsonObject?): String {
        val request = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", 1)
            put("method", method)
            if (params != null) {
                put("params", params)
            }
        }
        
        val response = httpClient.post("http://localhost:$testPort/") {
            contentType(ContentType.Application.Json)
            setBody(JsonSerializers.serialize(request))
        }
        
        return response.bodyAsText()
    }

    private fun createTestAdapter(): McpServerAdapter {
        return object : McpServerAdapter {
            override suspend fun getCapabilities(): McpServerCapabilities {
                return McpServerCapabilities(
                    prompts = McpServerCapability(listChanged = false),
                    tools = null
                )
            }

            override suspend fun listPrompts(): List<McpPrompt> {
                return listOf(
                    McpPrompt(
                        name = "test-prompt",
                        title = "Test Prompt",
                        description = "A test prompt"
                    )
                )
            }

            override suspend fun getPrompt(name: String, args: Map<String, String>): McpGetPromptResponse {
                return McpGetPromptResponse(
                    description = "Test prompt response",
                    messages = listOf(
                        MultimodalChatMessage(
                            role = tri.ai.core.MChatRole.User,
                            content = listOf(MChatMessagePart(MPartType.TEXT, text = "Test message"))
                        )
                    )
                )
            }

            override suspend fun listTools(): List<Executable> {
                return emptyList()
            }

            override suspend fun getTool(name: String): Executable? {
                return null
            }

            override suspend fun callTool(name: String, args: Map<String, String>): McpToolResult {
                return McpToolResult(name, null, null, null)
            }

            override suspend fun close() {
                // No-op for test
            }
        }
    }
}
