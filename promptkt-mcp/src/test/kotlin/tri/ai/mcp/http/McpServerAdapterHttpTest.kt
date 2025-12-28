package tri.ai.mcp.http

import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.request.receiveText
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
import org.junit.jupiter.api.Test

class McpServerAdapterHttpTest {

    companion object {
        private const val TEST_PORT = 9876
        private const val BASE_URL = "http://localhost:$TEST_PORT"
        private lateinit var server: NettyApplicationEngine

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

                        val response = when (method) {
                            "initialize" -> """{"jsonrpc":"2.0","id":$id,"result":{"protocolVersion":"2024-11-05","serverInfo":{"name":"test-server","version":"1.0.0"},"capabilities":{"prompts":{"listChanged":false},"tools":null}}}"""
                            "prompts/list" -> """{"jsonrpc":"2.0","id":$id,"result":{"prompts":[{"name":"test-prompt","title":"Test Prompt","description":"A test prompt"}]}}"""
                            "prompts/get" -> """{"jsonrpc":"2.0","id":$id,"result":{"description":"Test prompt response","messages":[{"role":"user","content":{"type":"text","text":"Test message"}}]}}"""
                            "tools/list" -> """{"jsonrpc":"2.0","id":$id,"result":{"tools":[{"name":"test-tool","description":"A test tool","version":"1.0.0","inputSchema":{},"outputSchema":{}}]}}"""
                            "tools/call" -> """{"jsonrpc":"2.0","id":$id,"result":{"content":[{"type":"text","text":"test output"}],"structuredContent":{"result":"test output"}}}"""
                            else -> """{"jsonrpc":"2.0","id":$id,"error":{"code":-32601,"message":"Method not found"}}"""
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
            val adapter = McpServerAdapterHttp(BASE_URL)

            val capabilities = adapter.getCapabilities()
            println("testGetCapabilities result: $capabilities")
            assertNotNull(capabilities)
            assertNotNull(capabilities?.prompts)
            assertEquals(false, capabilities?.prompts?.listChanged)

            adapter.close()
        }
    }

    @Test
    fun testListPrompts() {
        runTest {
            val adapter = McpServerAdapterHttp(BASE_URL)

            try {
                val prompts = adapter.listPrompts()
                println("testListPrompts result: $prompts")
                assertNotNull(prompts)
                assertTrue(prompts.size == 1, "Should have at least one prompt")
                assertEquals("test-prompt", prompts[0].name)
                assertEquals("Test Prompt", prompts[0].title)
            } finally {
                adapter.close()
            }
        }
    }

    @Test
    fun testGetPrompt() {
        runTest {
            val adapter = McpServerAdapterHttp(BASE_URL)

            try {
                val response = adapter.getPrompt("test-prompt", mapOf("arg1" to "value1"))
                println("testGetPrompt result: $response")
                assertNotNull(response)
                assertEquals("Test prompt response", response.description)
                assertTrue(response.messages.size == 1, "Should have at least one message")
            } finally {
                adapter.close()
            }
        }
    }

    @Test
    fun testListTools() {
        runTest {
            val adapter = McpServerAdapterHttp(BASE_URL)

            try {
                val tools = adapter.listTools()
                println("testListTools result: $tools")
                assertNotNull(tools)
                assertTrue(tools.size == 1, "Should have at least one tool")
                assertEquals("test-tool", tools[0].name)
            } finally {
                adapter.close()
            }
        }
    }

    @Test
    fun testGetTool() {
        runTest {
            val adapter = McpServerAdapterHttp(BASE_URL)

            try {
                val tool = adapter.getTool("test-tool")
                println("testGetTool result: $tool")
                assertNotNull(tool, "Tool should be found")
                assertEquals("test-tool", tool?.name)
            } finally {
                adapter.close()
            }
        }
    }

    @Test
    fun testCallTool() {
        runTest {
            val adapter = McpServerAdapterHttp(BASE_URL)
            try {
                val result = adapter.callTool("test-tool", mapOf("input" to "test"))
                println("testCallTool result: $result")
                assertNotNull(result)
                assertNotNull(result.content)
                assertNull(result.isError)
            } finally {
                adapter.close()
            }
        }
    }

    @Test
    fun testConnectionError() {
        runTest {
            val adapter = McpServerAdapterHttp("http://localhost:99999")

            try {
                try {
                    adapter.listPrompts()
                    fail("Should have thrown an exception")
                } catch (e: Exception) {
                    // Expected
                }
            } finally {
                adapter.close()
            }
        }
    }
}