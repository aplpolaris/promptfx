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
import tri.ai.mcp.McpJsonRpcHandler.Companion.METHOD_NOTIFICATIONS_PREFIX

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

    override suspend fun sendJsonRpcRequest(method: String, params: JsonObject?): JsonElement {
        initialize()
        return sendJsonRpcInitialization(method, params)
    }

    override suspend fun sendJsonRpcInitialization(method: String, params: JsonObject?): JsonElement {
        val request = buildJsonObject {
            put("jsonrpc", JsonPrimitive("2.0"))
            if (!method.startsWith(METHOD_NOTIFICATIONS_PREFIX))
                put("id", JsonPrimitive(requestId.getAndIncrement()))
            put("method", JsonPrimitive(method))
            if (params != null) {
                put("params", params)
            }
        }

        val response = httpClient.post("$baseUrl/mcp") {
            contentType(ContentType.Application.Json)
            setBody(JsonSerializers.serialize(request))
        }
        if (!response.status.isSuccess()) {
            throw McpException("HTTP request failed: ${response.status}")
        }

        val responseText = response.bodyAsText()
        if (responseText.isEmpty())
            return JsonNull
        val responseJson = Json.parseToJsonElement(responseText).jsonObject
        if (responseJson.containsKey("error")) {
            val error = responseJson["error"]?.jsonObject
            val message = error?.get("message")?.jsonPrimitive?.content ?: "Unknown error"
            throw McpException("JSON-RPC error: $message")
        }
        return responseJson["result"] ?: JsonNull
    }

    override suspend fun close() {
        httpClient.close()
    }

}
