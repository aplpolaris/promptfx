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
import tri.util.info
import tri.util.warning
import java.util.concurrent.ConcurrentHashMap

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
    private val sseScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
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
        val requestIdValue = request["id"]?.jsonPrimitive?.intOrNull

        // If SSE is enabled and connected, pre-register a pending response to avoid race conditions
        // where the SSE event arrives before we can register after receiving a queued response
        val pendingDeferred = if (enableSse && sseConnected && requestIdValue != null) {
            val deferred = CompletableDeferred<JsonElement>()
            pendingResponses[requestIdValue] = deferred
            deferred
        } else null

        val response = httpClient.post("$baseUrl/mcp") {
            contentType(ContentType.Application.Json)
            setBody(JsonSerializers.serialize(request))
            mcpSessionId?.let { header(HEADER_MCP_SESSION_ID, it) }
            timeout {
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
            }
        }

        if (!response.status.isSuccess()) {
            // Clean up pending response on failure
            if (requestIdValue != null) {
                info<McpProviderHttp>("HTTP request failed, removing pending response for request ID: $requestIdValue")
                pendingResponses.remove(requestIdValue)
            }
            throw McpException("HTTP request failed: ${response.status}")
        }
        
        // Always update the MCP session id if found
        val newSessionId = response.headers[HEADER_MCP_SESSION_ID]
        if (newSessionId != null && newSessionId != mcpSessionId) {
            mcpSessionId = newSessionId
            // Start SSE connection after getting session ID (only on first initialization)
            if (enableSse && !sseConnected && method == McpJsonRpcHandler.METHOD_INITIALIZE) {
                startSseConnection()
                // Give SSE a moment to establish connection
                delay(100)
            }
        }

        val responseText = response.bodyAsText()
        if (responseText.isEmpty()) {
            // If we have SSE enabled and a pending response registered, wait for it
            if (pendingDeferred != null) {
                info<McpProviderHttp>("Empty response received, waiting for SSE response for request ID: $requestIdValue")
                return waitForSseResponse(requestIdValue!!, pendingDeferred)
            }
            return JsonNull
        }
        
        val responseJson = Json.parseToJsonElement(responseText).jsonObject
        
        // Check if this is a valid JSON-RPC response
        val isJsonRpcResponse = responseJson.containsKey("jsonrpc") && 
                                (responseJson.containsKey("result") || responseJson.containsKey("error"))
        
        if (!isJsonRpcResponse) {
            // Response is not a JSON-RPC response (e.g., {"status":"queued"})
            // Wait for the actual response via SSE
            if (pendingDeferred != null) {
                info<McpProviderHttp>("Non-JSON-RPC response received: $responseJson, waiting for SSE response for request ID: $requestIdValue")
                return waitForSseResponse(requestIdValue!!, pendingDeferred)
            }
            // If SSE is not enabled or not connected, treat this as an error
            throw McpException("Invalid JSON-RPC response without result or error: $responseJson")
        }
        
        // We got a valid JSON-RPC response directly, so clean up the pending response if registered
        if (requestIdValue != null)
            pendingResponses.remove(requestIdValue)

        // Handle JSON-RPC error responses
        if (responseJson.containsKey("error")) {
            val error = responseJson["error"]?.jsonObject
            val message = error?.get("message")?.jsonPrimitive?.content ?: "Unknown error"
            throw McpException("JSON-RPC error: $message")
        }
        
        // Return the result
        return responseJson["result"]!!
    }

    private fun startSseConnection() {
        if (sseConnected) return
        
        val sessionId = mcpSessionId ?: run {
            warning<McpProviderHttp>("Cannot start SSE connection without session ID")
            return
        }

        info<McpProviderHttp>("Starting SSE connection with session ID: $sessionId")
        sseJob = sseScope.launch {
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
                    info<McpProviderHttp>("SSE connection established")
                    
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
            info<McpProviderHttp>("SSE connection closed")
        }
    }

    private suspend fun handleSseEvent(eventType: String?, data: String) {
        info<McpProviderHttp>("Received SSE event: type=$eventType, data=$data")
        
        // Some event types (like endpoint events) may send plain text, not JSON
        // Only attempt to parse as JSON for message events or if no event type is specified
        if (eventType != null && eventType != "message") {
            info<McpProviderHttp>("Non-message event type '$eventType', skipping JSON parsing")
            return
        }
        
        try {
            val json = try {
                Json.parseToJsonElement(data).jsonObject
            } catch (e: Exception) {
                throw McpSseParseException(
                    "Failed to parse SSE event data as JSON",
                    eventType,
                    data,
                    e
                )
            }
            
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
                    } else {
                        info<McpProviderHttp>("Received JSON-RPC response for request ID $id but no pending request found")
                    }
                } else {
                    throw McpSseInvalidDataException(
                        "JSON-RPC response has invalid or missing 'id' field",
                        eventType,
                        data
                    )
                }
            }
            
            // Queue other messages for processing
            info<McpProviderHttp>("Queueing non-response message for processing")
            sseMessageQueue.send(json)
        } catch (e: McpSseParseException) {
            warning<McpProviderHttp>("SSE parse error (type=${e.eventType}): ${e.message}")
        } catch (e: McpSseInvalidDataException) {
            warning<McpProviderHttp>("SSE invalid data (type=${e.eventType}): ${e.message}")
        } catch (e: Exception) {
            warning<McpProviderHttp>("Unexpected error handling SSE event (type=$eventType): ${e.message}")
        }
    }

    private suspend fun waitForSseResponse(requestId: Int, deferred: CompletableDeferred<JsonElement>, timeoutMs: Long = REQUEST_TIMEOUT_MS): JsonElement {
        return withTimeoutOrNull(timeoutMs) {
            deferred.await()
        } ?: run {
            pendingResponses.remove(requestId)
            throw McpException("Timeout waiting for SSE response for request ID: $requestId")
        }
    }

    override suspend fun close() {
        sseJob?.cancel()
        sseScope.cancel()
        sseConnected = false
        sseMessageQueue.close()
        
        // Complete all pending responses exceptionally to prevent hanging
        pendingResponses.values.forEach { deferred ->
            deferred.completeExceptionally(McpException("Connection closed"))
        }
        pendingResponses.clear()
        
        httpClient.close()
    }

    companion object {
        const val HEADER_MCP_SESSION_ID = "Mcp-Session-Id"
        private const val REQUEST_TIMEOUT_MS = 30000L // 30 seconds
    }

}
