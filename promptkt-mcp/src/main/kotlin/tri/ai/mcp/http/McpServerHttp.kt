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

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import tri.ai.mcp.JsonRpcHandler
import tri.ai.mcp.JsonSerializers
import tri.ai.mcp.McpProvider
import tri.ai.mcp.McpJsonRpcHandler

/**
 * MCP server that runs over http -- switch out for a library when possible.
 * Uses [McpProvider] for the underlying server implementation,
 * allowing the server to be defined in-memory or to mirror a remote server.
 */
class McpServerHttp(private val server: McpProvider, private val port: Int = 8080) {

    private val businessLogic = McpJsonRpcHandler(server)
    private val router = McpServerHttpRouter(businessLogic)

    /** Start the HTTP server. */
    fun startServer() {
        router.startServer(port)
    }

    suspend fun close() {
        server.close()
        router.close()
    }

}
/**
 * Handles JSON-RPC 2.0 message routing over HTTP.
 * Separates HTTP request handling and response writing from business logic.
 */
class McpServerHttpRouter(private val handler: JsonRpcHandler) {

    private var server: NettyApplicationEngine? = null

    /** Start the HTTP server on the given port. */
    fun startServer(port: Int = 8080) {
        server = embeddedServer(Netty, port = port) {
            routing {
                post("/mcp") {
                    handleJsonRpcRequest(call)
                }
                get("/health") {
                    call.respondText("OK", ContentType.Text.Plain, HttpStatusCode.Companion.OK)
                }
            }
        }.start(wait = false)
    }

    private suspend fun handleJsonRpcRequest(call: ApplicationCall) {
        try {
            val requestBody = call.receiveText()
            if (requestBody.isBlank()) {
                call.respondJsonRpcError(null, -32700, "Parse error: empty request")
                return
            }

            val req = runCatching { JsonSerializers.parseRequest(requestBody) }.getOrElse {
                call.respondJsonRpcError(null, -32700, "Parse error")
                return
            }

            val id = req["id"]
            val method = req["method"]?.jsonPrimitive?.contentOrNull
            val params = req["params"]?.jsonObject

            println("Received JSON-RPC request: method=$method, id=$id")

            try {
                val result = handler.handleRequest(method, params)
                if (result != null) {
                    call.respondJsonRpcResult(id, result)
                } else if (method == McpJsonRpcHandler.METHOD_NOTIFICATIONS_CLOSE) {
                    // Special case: close notification should exit
                    // Respond first, then close to avoid race condition
                    call.respond(HttpStatusCode.NoContent)
                    delay(100) // Allow response to be sent
                    close()
                } else if (method?.startsWith(McpJsonRpcHandler.METHOD_NOTIFICATIONS_PREFIX) == true) {
                    // Other notifications have no response
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respondJsonRpcError(id, -32601, "Method not found: $method")
                }
            } catch (t: Throwable) {
                call.respondJsonRpcError(id, -32603, "Internal error: ${t.message ?: t::class.simpleName}")
            }
        } catch (e: Exception) {
            call.respondJsonRpcError(null, -32603, "Internal error: ${e.message ?: e::class.simpleName}")
        }
    }

    private suspend fun ApplicationCall.respondJsonRpcResult(id: JsonElement?, result: JsonElement) {
        val resp = buildJsonObject {
            put("jsonrpc", JsonPrimitive("2.0"))
            if (id != null) put("id", id)
            put("result", result)
        }
        println("Responding with JSON-RPC result: id=$id")
        respondText(JsonSerializers.serialize(resp), ContentType.Application.Json, HttpStatusCode.Companion.OK)
    }

    private suspend fun ApplicationCall.respondJsonRpcError(id: JsonElement?, code: Int, message: String) {
        val err = buildJsonObject {
            put("jsonrpc", JsonPrimitive("2.0"))
            if (id != null) put("id", id)
            put("error", buildJsonObject {
                put("code", JsonPrimitive(code))
                put("message", JsonPrimitive(message))
            })
        }
        println("Responding with JSON-RPC error: id=$id, code=$code, message=$message")
        respondText(JsonSerializers.serialize(err), ContentType.Application.Json, HttpStatusCode.OK)
    }

    fun close() {
        println("Stopping MCP HTTP Server...")
        server?.stop(1000, 2000)
    }

}
