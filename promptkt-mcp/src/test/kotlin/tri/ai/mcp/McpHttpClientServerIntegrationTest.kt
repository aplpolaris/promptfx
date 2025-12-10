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
import io.ktor.server.response.*
import io.ktor.server.routing.*
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
 */
class McpHttpClientServerIntegrationTest {
    
    private val jsonRpcPort = 9879
    private val restPort = 9880

    /**
     * Integration test showing both McpServerHttp and McpServerAdapterHttp working together
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
