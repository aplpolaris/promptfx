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
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tri.ai.mcp.tool.StarterToolLibrary
import tri.ai.prompt.PromptLibrary

/**
 * Integration test that verifies the full HTTP MCP server workflow:
 * - Uses [McpServerHttp] to stand up a JSON-RPC HTTP server with an embedded adapter
 * - Uses an HTTP client to send JSON-RPC requests to the server
 * - Validates both server-side (McpServerHttp) and client-side functionality
 * 
 * Note: McpServerHttp uses JSON-RPC 2.0 over HTTP, while McpServerAdapterHttp
 * is designed for REST-style HTTP APIs. This test validates the JSON-RPC server.
 */
class McpServerHttpIntegrationTest {

    private lateinit var mcpServer: McpServerHttp
    private lateinit var httpClient: HttpClient
    private val testPort = 9878
    private val baseUrl = "http://localhost:$testPort"

    @BeforeEach
    fun setup() {
        runTest {
            // Create an embedded MCP server with prompts and tools
            val embeddedAdapter = McpServerEmbedded(
                PromptLibrary.INSTANCE,
                StarterToolLibrary()
            )
            
            // Start the HTTP server wrapping the embedded adapter
            mcpServer = McpServerHttp(embeddedAdapter, testPort)
            mcpServer.startServer()
            
            // Wait for server to be ready
            delay(1000)
            
            // Create an HTTP client for JSON-RPC requests
            httpClient = HttpClient(OkHttp)
        }
    }

    @AfterEach
    fun teardown() {
        runTest {
            httpClient.close()
            mcpServer.close()
        }
    }
    
    /**
     * Send a JSON-RPC 2.0 request to the server
     */
    private suspend fun sendJsonRpcRequest(method: String, params: JsonObject?): JsonElement {
        val request = buildJsonObject {
            put("jsonrpc", JsonPrimitive("2.0"))
            put("id", JsonPrimitive(1))
            put("method", JsonPrimitive(method))
            if (params != null) {
                put("params", params)
            }
        }
        
        val response = httpClient.post(baseUrl) {
            contentType(ContentType.Application.Json)
            setBody(JsonSerializers.serialize(request))
        }
        
        val responseText = response.bodyAsText()
        val responseJson = Json.parseToJsonElement(responseText).jsonObject
        
        // Check for errors
        if (responseJson.containsKey("error")) {
            val error = responseJson["error"]?.jsonObject
            val message = error?.get("message")?.jsonPrimitive?.content ?: "Unknown error"
            throw McpServerException("JSON-RPC error: $message")
        }
        
        return responseJson["result"] ?: JsonNull
    }

    @Test
    fun testInitialize() {
        runTest {
            val result = sendJsonRpcRequest("initialize", buildJsonObject {
                put("protocolVersion", JsonPrimitive("2024-11-05"))
                put("capabilities", buildJsonObject {})
                put("clientInfo", buildJsonObject {
                    put("name", JsonPrimitive("test-client"))
                    put("version", JsonPrimitive("1.0.0"))
                })
            })
            
            val resultObj = result.jsonObject
            assertTrue(resultObj.containsKey("protocolVersion"), "Should return protocol version")
            assertTrue(resultObj.containsKey("serverInfo"), "Should return server info")
            assertTrue(resultObj.containsKey("capabilities"), "Should return capabilities")
        }
    }

    @Test
    fun testListPrompts() {
        runTest {
            val result = sendJsonRpcRequest("prompts/list", null)
            
            val resultObj = result.jsonObject
            assertTrue(resultObj.containsKey("prompts"), "Should have prompts key")
            
            val prompts = resultObj["prompts"]?.jsonArray
            assertNotNull(prompts, "Prompts should not be null")
            assertTrue(prompts!!.isNotEmpty(), "Should have at least one prompt from the embedded library")
            
            // Verify prompt structure
            val firstPrompt = prompts[0].jsonObject
            assertTrue(firstPrompt.containsKey("name"), "Prompt should have a name")
        }
    }

    @Test
    fun testGetPrompt() {
        runTest {
            // First, get the list of available prompts
            val listResult = sendJsonRpcRequest("prompts/list", null)
            val prompts = listResult.jsonObject["prompts"]?.jsonArray
            assertNotNull(prompts)
            assertTrue(prompts!!.isNotEmpty(), "Should have prompts to test")
            
            val promptName = prompts[0].jsonObject["name"]?.jsonPrimitive?.content
            assertNotNull(promptName)
            
            // Get the prompt with no arguments
            val result = sendJsonRpcRequest("prompts/get", buildJsonObject {
                put("name", JsonPrimitive(promptName!!))
                put("arguments", buildJsonObject {})
            })
            
            val resultObj = result.jsonObject
            assertTrue(resultObj.containsKey("messages"), "Response should contain messages")
            
            val messages = resultObj["messages"]?.jsonArray
            assertNotNull(messages)
            assertTrue(messages!!.isNotEmpty(), "Response should have at least one message")
        }
    }

    @Test
    fun testGetPromptWithArguments() {
        runTest {
            // Try to get a prompt that accepts arguments
            try {
                val result = sendJsonRpcRequest("prompts/get", buildJsonObject {
                    put("name", JsonPrimitive("text-qa/answer"))
                    put("arguments", buildJsonObject {
                        put("instruct", JsonPrimitive("What is the capital of France?"))
                        put("input", JsonPrimitive("Paris is a beautiful city in France."))
                    })
                })
                
                val resultObj = result.jsonObject
                assertTrue(resultObj.containsKey("messages"), "Response should contain messages")
                
                val messages = resultObj["messages"]?.jsonArray
                assertNotNull(messages)
                assertTrue(messages!!.isNotEmpty(), "Response should have at least one message")
            } catch (e: McpServerException) {
                // If the prompt doesn't exist, test passes since we're testing the integration
                // not the specific prompt library content
                assertTrue(e.message?.contains("not found") ?: false)
            }
        }
    }

    @Test
    fun testListTools() {
        runTest {
            val result = sendJsonRpcRequest("tools/list", null)
            
            val resultObj = result.jsonObject
            assertTrue(resultObj.containsKey("tools"), "Should have tools key")
            
            val tools = resultObj["tools"]?.jsonArray
            assertNotNull(tools, "Tools should not be null")
            assertTrue(tools!!.isNotEmpty(), "Should have at least one tool from the starter library")
            
            // Verify tool structure
            val firstTool = tools[0].jsonObject
            assertTrue(firstTool.containsKey("name"), "Tool should have a name")
            assertTrue(firstTool.containsKey("description"), "Tool should have a description")
        }
    }

    @Test
    fun testCallTool() {
        runTest {
            // First, get the list of available tools
            val listResult = sendJsonRpcRequest("tools/list", null)
            val tools = listResult.jsonObject["tools"]?.jsonArray
            assertNotNull(tools)
            assertTrue(tools!!.isNotEmpty(), "Should have tools to test")
            
            val toolName = tools[0].jsonObject["name"]?.jsonPrimitive?.content
            assertNotNull(toolName)
            
            // Call the tool with empty arguments
            val result = sendJsonRpcRequest("tools/call", buildJsonObject {
                put("name", JsonPrimitive(toolName!!))
                put("arguments", buildJsonObject {})
            })
            
            val resultObj = result.jsonObject
            assertTrue(resultObj.containsKey("content"), "Tool result should have content")
        }
    }

    @Test
    fun testEndToEndWorkflow() {
        runTest {
            // Test a complete workflow: initialize -> list prompts -> get prompt -> list tools -> call tool
            
            // 1. Initialize
            val initResult = sendJsonRpcRequest("initialize", buildJsonObject {
                put("protocolVersion", JsonPrimitive("2024-11-05"))
                put("capabilities", buildJsonObject {})
                put("clientInfo", buildJsonObject {
                    put("name", JsonPrimitive("test-client"))
                    put("version", JsonPrimitive("1.0.0"))
                })
            })
            assertNotNull(initResult)
            
            // 2. List prompts
            val promptsResult = sendJsonRpcRequest("prompts/list", null)
            val prompts = promptsResult.jsonObject["prompts"]?.jsonArray
            assertTrue(prompts!!.isNotEmpty())
            
            // 3. Get a specific prompt
            val promptName = prompts[0].jsonObject["name"]?.jsonPrimitive?.content
            val promptResult = sendJsonRpcRequest("prompts/get", buildJsonObject {
                put("name", JsonPrimitive(promptName!!))
                put("arguments", buildJsonObject {})
            })
            assertNotNull(promptResult)
            
            // 4. List tools
            val toolsResult = sendJsonRpcRequest("tools/list", null)
            val tools = toolsResult.jsonObject["tools"]?.jsonArray
            assertTrue(tools!!.isNotEmpty())
            
            // 5. Call a tool
            val toolName = tools[0].jsonObject["name"]?.jsonPrimitive?.content
            val toolResult = sendJsonRpcRequest("tools/call", buildJsonObject {
                put("name", JsonPrimitive(toolName!!))
                put("arguments", buildJsonObject {})
            })
            assertNotNull(toolResult)
        }
    }
}
