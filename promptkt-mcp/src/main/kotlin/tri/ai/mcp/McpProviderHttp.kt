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
package tri.ai.mcp

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.*
import tri.ai.mcp.McpJsonRpcHandler.Companion.buildJsonRpc
import tri.util.fine
import tri.util.warning
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * MCP provider for an external Streamable HTTP MCP server.
 * Follows the MCP specification for Streamable HTTP transport: https://modelcontextprotocol.io/specification/2025-06-18/basic/transports#streamable-http
 * 
 * This implementation supports:
 * - Initialization sequence with session ID management
 * - SSE (Server-Sent Events) channel via GET to /mcp for receiving async server messages
 * - POST requests to /mcp for sending commands (existing behavior)
 * - Handling both synchronous JSON-RPC responses and asynchronous SSE messages
 */
class McpProviderHttp(_baseUrl: String, private val enableSse: Boolean = true) : McpProviderSupport() {

    private val baseUrl = _baseUrl.removeSuffix("/mcp").trimEnd('/')
    private val httpClient = HttpClient(CIO) {
        engine {
            requestTimeout = 0 // No timeout for SSE connections
        }
        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
        }
    }
    private var mcpSessionId: String? = null
    
    // SSE support
    private var sseJob: Job? = null
    private val sseMessageQueue = Channel<JsonObject>(Channel.UNLIMITED)
    private val pendingResponses = ConcurrentHashMap<Int, CompletableDeferred<JsonElement>>()
    private var sseConnected = false

    override suspend fun sendJsonRpcWithInitializationCheck(method: String, params: JsonObject?): JsonElement {
        initialize()
        return sendJsonRpc(method, params)
    }

    override suspend fun sendJsonRpc(method: String, params: JsonObject?): JsonElement {
        val request = buildJsonRpc(method, requestId, params)
        val reqId = request["id"]?.jsonPrimitive?.intOrNull

        val response = httpClient.post("$baseUrl/mcp") {
            contentType(ContentType.Application.Json)
            setBody(JsonSerializers.serialize(request))
            mcpSessionId?.let { header(HEADER_MCP_SESSION_ID, it) }
            timeout {
                requestTimeoutMillis = 30000 // 30 seconds for POST requests
            }
        }

        if (!response.status.isSuccess())
            throw McpException("HTTP request failed: ${response.status}")
        
        // Always update the MCP session id if found
        val newSessionId = response.headers[HEADER_MCP_SESSION_ID]
        if (newSessionId != null && newSessionId != mcpSessionId) {
            mcpSessionId = newSessionId
            // Start SSE connection after getting session ID (only on first initialization)
            if (enableSse && !sseConnected && method == McpJsonRpcHandler.Companion.METHOD_INITIALIZE) {
                startSseConnection()
            }
        }

        val responseText = response.bodyAsText()
        if (responseText.isEmpty()) {
            // If we have SSE enabled and a request ID, wait for response from SSE
            if (enableSse && sseConnected && reqId != null) {
                return waitForSseResponse(reqId)
            }
            return JsonNull
        }
        
        val responseJson = Json.parseToJsonElement(responseText).jsonObject
        if (responseJson.containsKey("error")) {
            val error = responseJson["error"]?.jsonObject
            val message = error?.get("message")?.jsonPrimitive?.content ?: "Unknown error"
            throw McpException("JSON-RPC error: $message")
        }
        if (!responseJson.containsKey("result")) {
            throw McpException("Invalid JSON-RPC response without result: $responseJson")
        }
        return responseJson["result"]!!
    }

    private fun startSseConnection() {
        if (sseConnected) return
        
        val sessionId = mcpSessionId ?: run {
            warning<McpProviderHttp>("Cannot start SSE connection without session ID")
            return
        }

        fine<McpProviderHttp>("Starting SSE connection with session ID: $sessionId")
        sseJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                httpClient.prepareGet("$baseUrl/mcp") {
                    header(HEADER_MCP_SESSION_ID, sessionId)
                    header(HttpHeaders.Accept, "text/event-stream")
                    header(HttpHeaders.CacheControl, "no-cache")
                }.execute { response ->
                    if (!response.status.isSuccess()) {
                        warning<McpProviderHttp>("SSE connection failed: ${response.status}")
                        return@execute
                    }

                    sseConnected = true
                    fine<McpProviderHttp>("SSE connection established")
                    
                    val channel = response.bodyAsChannel()
                    processSseStream(channel)
                }
            } catch (e: Exception) {
                warning<McpProviderHttp>("SSE connection error: ${e.message}")
                sseConnected = false
            }
        }
    }

    private suspend fun processSseStream(channel: ByteReadChannel) {
        val buffer = StringBuilder()
        var eventType: String? = null
        var eventData = StringBuilder()

        try {
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                
                when {
                    line.startsWith("event:") -> {
                        eventType = line.substring(6).trim()
                    }
                    line.startsWith("data:") -> {
                        val data = line.substring(5).trim()
                        if (eventData.isNotEmpty()) {
                            eventData.append("\n")
                        }
                        eventData.append(data)
                    }
                    line.isEmpty() -> {
                        // End of event
                        if (eventData.isNotEmpty()) {
                            handleSseEvent(eventType, eventData.toString())
                            eventType = null
                            eventData = StringBuilder()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            warning<McpProviderHttp>("Error processing SSE stream: ${e.message}")
        } finally {
            sseConnected = false
            fine<McpProviderHttp>("SSE connection closed")
        }
    }

    private suspend fun handleSseEvent(eventType: String?, data: String) {
        fine<McpProviderHttp>("Received SSE event: type=$eventType, data=$data")
        
        try {
            val json = Json.parseToJsonElement(data).jsonObject
            
            // Check if this is a JSON-RPC response
            if (json.containsKey("jsonrpc") && json.containsKey("id")) {
                val id = json["id"]?.jsonPrimitive?.intOrNull
                if (id != null) {
                    val pending = pendingResponses.remove(id)
                    if (pending != null) {
                        if (json.containsKey("error")) {
                            val error = json["error"]?.jsonObject
                            val message = error?.get("message")?.jsonPrimitive?.content ?: "Unknown error"
                            pending.completeExceptionally(McpException("JSON-RPC error: $message"))
                        } else {
                            pending.complete(json["result"] ?: JsonNull)
                        }
                        return
                    }
                }
            }
            
            // Queue other messages for processing
            sseMessageQueue.send(json)
        } catch (e: Exception) {
            warning<McpProviderHttp>("Error parsing SSE event data: ${e.message}")
        }
    }

    private suspend fun waitForSseResponse(requestId: Int, timeoutMs: Long = 30000): JsonElement {
        val deferred = CompletableDeferred<JsonElement>()
        pendingResponses[requestId] = deferred
        
        return withTimeoutOrNull(timeoutMs) {
            deferred.await()
        } ?: run {
            pendingResponses.remove(requestId)
            throw McpException("Timeout waiting for SSE response for request ID: $requestId")
        }
    }

    override suspend fun close() {
        sseJob?.cancel()
        sseConnected = false
        sseMessageQueue.close()
        pendingResponses.clear()
        httpClient.close()
    }

    companion object {
        const val HEADER_MCP_SESSION_ID = "Mcp-Session-Id"
    }

}
