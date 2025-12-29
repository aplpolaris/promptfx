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
import kotlinx.serialization.json.*
import tri.ai.mcp.McpJsonRpcHandler.Companion.buildJsonRpc

/**
 * MCP provider for an external Streamable HTTP MCP server.
 * Follows the MCP specification for Streamable HTTP transport: https://modelcontextprotocol.io/specification/2025-06-18/basic/transports#streamable-http
 */
class McpProviderHttp(_baseUrl: String) : McpProviderSupport() {

    private val baseUrl = _baseUrl.removeSuffix("/mcp").trimEnd('/')
    private val httpClient = HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(true)
            }
        }
    }
    private var mcpSessionId: String? = null

    override suspend fun sendJsonRpcWithInitializationCheck(method: String, params: JsonObject?): JsonElement {
        initialize()
        return sendJsonRpc(method, params)
    }

    override suspend fun sendJsonRpc(method: String, params: JsonObject?): JsonElement {
        val request = buildJsonRpc(method, requestId, params)

        val response = httpClient.post("$baseUrl/mcp") {
            contentType(ContentType.Application.Json)
            setBody(JsonSerializers.serialize(request))
            mcpSessionId?.let { header(HEADER_MCP_SESSION_ID, it) }
        }

        if (!response.status.isSuccess())
            throw McpException("HTTP request failed: ${response.status}")
        // always update the MCP session id if found
        response.headers[HEADER_MCP_SESSION_ID]?.let { mcpSessionId = it }

        val responseText = response.bodyAsText()
        if (responseText.isEmpty())
            return JsonNull
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

    override suspend fun close() {
        httpClient.close()
    }

    companion object {
        const val HEADER_MCP_SESSION_ID = "Mcp-Session-Id"
    }

}
