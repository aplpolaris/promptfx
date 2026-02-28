/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tri.ai.core.MultimodalChatMessage
import tri.ai.mcp.*
import tri.ai.mcp.tool.McpToolMetadata
import tri.ai.mcp.tool.McpToolResponse

class McpServerHttpTest {

    private lateinit var mcpServer: McpServerHttp
    private lateinit var httpClient: HttpClient
    private val testPort = 9877

    @BeforeEach
    fun setup() {
        httpClient = HttpClient(OkHttp)
        val provider = createTestProvider()
        mcpServer = McpServerHttp(provider, testPort)
        mcpServer.startServer()
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
                val response = httpClient.post("http://localhost:$testPort/mcp") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"jsonrpc":"2.0","id":0,"method":"ping"}""")
                }
                if (response.status.isSuccess() || response.status == HttpStatusCode.Companion.OK) {
                    return
                }
            } catch (e: Exception) {
                if (attempt < maxAttempts - 1) {
                    delay(100)
                } else {
                    throw Exception("Server failed to start after $maxAttempts attempts", e)
                }
            }
        }
    }

    @Test
    fun testStartServerAndInitialize() {
        runTest {
            waitForServerReady()

            val response = sendJsonRpcRequest("initialize", null)
            println(response)

            Assertions.assertTrue(response.contains("\"protocolVersion\""))
            Assertions.assertTrue(response.contains("\"serverInfo\""))
            Assertions.assertTrue(response.contains("promptfx-prompts"))
        }
    }

    @Test
    fun testHealthCheck() {
        runTest {
            waitForServerReady()

            val response = httpClient.get("http://localhost:$testPort/health")

            Assertions.assertEquals(HttpStatusCode.Companion.OK, response.status)
            Assertions.assertEquals("OK", response.bodyAsText())
        }
    }

    @Test
    fun testListPrompts() {
        runTest {
            waitForServerReady()

            val response = sendJsonRpcRequest("prompts/list", null)
            println(response)

            Assertions.assertTrue(response.contains("\"prompts\""))
            Assertions.assertTrue(response.contains("test-prompt"))
        }
    }

    @Test
    fun testGetPrompt() {
        runTest {
            waitForServerReady()

            val params = buildJsonObject {
                put("name", "test-prompt")
                put("arguments", buildJsonObject {})
            }

            val response = sendJsonRpcRequest("prompts/get", params)
            println(response)

            Assertions.assertTrue(response.contains("\"messages\""))
        }
    }

    @Test
    fun testListTools() {
        runTest {
            waitForServerReady()

            val response = sendJsonRpcRequest("tools/list", null)
            println(response)

            Assertions.assertTrue(response.contains("\"tools\""))
        }
    }

    @Test
    fun testInvalidMethod() {
        runTest {
            waitForServerReady()

            val response = sendJsonRpcRequest("invalid/method", null)
            println(response)

            Assertions.assertTrue(response.contains("\"error\""))
            Assertions.assertTrue(response.contains("-32601") || response.contains("Method not found"))
        }
    }

    @Test
    fun testMalformedRequest() {
        runTest {
            waitForServerReady()

            val response = httpClient.post("http://localhost:$testPort/mcp") {
                contentType(ContentType.Application.Json)
                setBody("{not valid json")
            }
            println(response)

            val responseText = response.bodyAsText()
            Assertions.assertTrue(responseText.contains("\"error\""))
            Assertions.assertTrue(responseText.contains("-32700") || responseText.contains("Parse error"))
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

        val response = httpClient.post("http://localhost:$testPort/mcp") {
            contentType(ContentType.Application.Json)
            setBody(JsonSerializers.serialize(request))
        }

        return response.bodyAsText()
    }

    private fun createTestProvider() = object : McpProvider {
        override suspend fun initialize() {}
        override suspend fun getCapabilities() = McpCapabilities(
            prompts = McpCapability(listChanged = false),
            tools = null
        )
        override suspend fun listPrompts() = listOf(
            McpPrompt(
                name = "test-prompt",
                title = "Test Prompt",
                description = "A test prompt"
            )
        )

        override suspend fun getPrompt(name: String, args: Map<String, String>) =
            McpPromptResponse(
                description = "Test prompt response",
                messages = listOf(MultimodalChatMessage.Companion.user("Test message"))
            )

        override suspend fun listTools(): List<McpToolMetadata> = emptyList()

        override suspend fun getTool(name: String): McpToolMetadata? = null

        override suspend fun callTool(name: String, args: Map<String, Any?>) = McpToolResponse.error("Unsupported")

        override suspend fun listResources(): List<McpResource> = emptyList()

        override suspend fun listResourceTemplates(): List<McpResourceTemplate> = emptyList()

        override suspend fun readResource(uri: String): McpResourceResponse {
            throw McpException("Resource not found: $uri")
        }

        override suspend fun close() {
            // No-op for test
        }
    }
}
