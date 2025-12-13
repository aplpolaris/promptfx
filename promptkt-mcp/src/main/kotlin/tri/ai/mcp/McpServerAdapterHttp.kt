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

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.annotations.Beta
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import tri.ai.core.tool.Executable
import tri.ai.mcp.tool.McpToolResult
import tri.ai.mcp.tool.StubTool
import tri.util.json.jsonMapper
import java.util.concurrent.atomic.AtomicLong

/**
 * Remote MCP server adapter that connects to external MCP servers via HTTP using JSON-RPC 2.0.
 * Follows the MCP specification for HTTP transport: https://modelcontextprotocol.io/specification/2025-06-18/basic/transports#streamable-http
 */
@Beta
class McpServerAdapterHttp(private val baseUrl: String) : McpServerAdapter {
    
    private val httpClient = HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(true)
            }
        }
    }
    

    private val requestId = AtomicLong(1)
    
    /**
     * Send a JSON-RPC request to the MCP server at /mcp endpoint
     */
    private suspend fun sendJsonRpcRequest(method: String, params: JsonObject? = null): JsonElement {
        val request = buildJsonObject {
            put("jsonrpc", JsonPrimitive("2.0"))
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
            throw McpServerException("HTTP request failed: ${response.status}")
        }
        
        val responseText = response.bodyAsText()
        val responseJson = Json.parseToJsonElement(responseText).jsonObject
        
        // Check for JSON-RPC error
        if (responseJson.containsKey("error")) {
            val error = responseJson["error"]?.jsonObject
            val message = error?.get("message")?.jsonPrimitive?.content ?: "Unknown error"
            throw McpServerException("JSON-RPC error: $message")
        }
        
        return responseJson["result"] ?: JsonNull
    }

    override suspend fun getCapabilities(): McpServerCapabilities? {
        try {
            val result = sendJsonRpcRequest("initialize", buildJsonObject {
                put("protocolVersion", JsonPrimitive("2024-11-05"))
                put("capabilities", buildJsonObject {})
                put("clientInfo", buildJsonObject {
                    put("name", JsonPrimitive("promptfx-http-client"))
                    put("version", JsonPrimitive("0.1.0"))
                })
            })
            
            val capabilitiesJson = result.jsonObject["capabilities"]?.jsonObject ?: return null
            return jsonMapper.readValue(JsonSerializers.serialize(capabilitiesJson))
        } catch (e: Exception) {
            // Capabilities endpoint is optional, so we don't throw an error
            return null
        }
    }
    
    override suspend fun listPrompts(): List<McpPrompt> {
        try {
            val result = sendJsonRpcRequest("prompts/list")
            val promptsJson = result.jsonObject["prompts"]?.jsonArray
                ?: throw McpServerException("No prompts in response")
            
            return jsonMapper.readValue(JsonSerializers.serialize(promptsJson))
        } catch (e: McpServerException) {
            throw e
        } catch (e: Exception) {
            throw McpServerException("Error connecting to MCP server: ${e.message}")
        }
    }
    
    override suspend fun getPrompt(name: String, args: Map<String, String>): McpGetPromptResponse {
        try {
            val params = buildJsonObject {
                put("name", JsonPrimitive(name))
                put("arguments", buildJsonObject {
                    args.forEach { (key, value) ->
                        put(key, JsonPrimitive(value))
                    }
                })
            }
            
            val result = sendJsonRpcRequest("prompts/get", params)
            
            // Convert lowercase roles from MCP spec to capitalized roles for internal enum
            val resultWithCapitalizedRoles = capitalizeRolesInResult(result)
            
            return jsonMapper.readValue(JsonSerializers.serialize(resultWithCapitalizedRoles))
        } catch (e: McpServerException) {
            throw e
        } catch (e: Exception) {
            throw McpServerException("Error getting prompt from MCP server: ${e.message}")
        }
    }
    
    /**
     * Convert lowercase roles from MCP spec ("user", "assistant") to capitalized form ("User", "Assistant")
     * and convert content from single object to array format needed by internal MChatRole enum
     */
    private fun capitalizeRolesInResult(result: JsonElement): JsonElement {
        if (result !is JsonObject) return result
        
        val messages = result["messages"]?.jsonArray ?: return result
        val updatedMessages = buildJsonArray {
            for (message in messages) {
                if (message is JsonObject) {
                    val role = message["role"]?.jsonPrimitive?.content
                    val content = message["content"]
                    
                    addJsonObject {
                        message.forEach { (key, value) ->
                            when (key) {
                                "role" -> {
                                    // Capitalize first letter for internal enum
                                    put(key, JsonPrimitive(role?.replaceFirstChar { it.uppercase() } ?: "User"))
                                }
                                "content" -> {
                                    // Convert single content object to array if needed
                                    if (content is JsonObject) {
                                        put(key, buildJsonArray { add(convertMcpContentToInternal(content)) })
                                    } else if (content is JsonArray) {
                                        put(key, buildJsonArray {
                                            content.forEach { add(convertMcpContentToInternal(it)) }
                                        })
                                    } else {
                                        put(key, value)
                                    }
                                }
                                else -> put(key, value)
                            }
                        }
                    }
                } else {
                    add(message)
                }
            }
        }
        
        return buildJsonObject {
            result.forEach { (key, value) ->
                if (key == "messages") {
                    put(key, updatedMessages)
                } else {
                    put(key, value)
                }
            }
        }
    }
    
    /**
     * Convert MCP content format to internal format
     */
    private fun convertMcpContentToInternal(content: JsonElement): JsonObject {
        if (content !is JsonObject) {
            return buildJsonObject {
                put("partType", JsonPrimitive("TEXT"))
                put("text", JsonPrimitive(content.toString()))
            }
        }
        
        val type = content["type"]?.jsonPrimitive?.content
        return buildJsonObject {
            when (type) {
                "text" -> {
                    put("partType", JsonPrimitive("TEXT"))
                    content["text"]?.let { put("text", it) }
                }
                "image" -> {
                    put("partType", JsonPrimitive("IMAGE"))
                    content["data"]?.let { put("inlineData", it) }
                    content["mimeType"]?.let { put("mimeType", it) }
                }
                "audio" -> {
                    put("partType", JsonPrimitive("AUDIO"))
                    content["data"]?.let { put("inlineData", it) }
                    content["mimeType"]?.let { put("mimeType", it) }
                }
                else -> {
                    put("partType", JsonPrimitive("TEXT"))
                    put("text", JsonPrimitive(content.toString()))
                }
            }
        }
    }

    override suspend fun listTools(): List<Executable> {
        try {
            val result = sendJsonRpcRequest("tools/list")
            val toolsJson = result.jsonObject["tools"]?.jsonArray
                ?: throw McpServerException("No tools in response")
            
            // Tools are returned as StubTool instances from MCP servers
            val toolsList = mutableListOf<Executable>()
            for (toolElement in toolsJson) {
                // Add hardCodedOutput field if it's missing (required by StubTool)
                val toolWithHardCodedOutput = if (toolElement is JsonObject && !toolElement.containsKey("hardCodedOutput")) {
                    buildJsonObject {
                        toolElement.forEach { key, value -> put(key, value) }
                        put("hardCodedOutput", buildJsonObject {})
                    }
                } else {
                    toolElement
                }
                val tool = jsonMapper.readValue<StubTool>(JsonSerializers.serialize(toolWithHardCodedOutput))
                toolsList.add(tool)
            }
            return toolsList
        } catch (e: McpServerException) {
            throw e
        } catch (e: Exception) {
            throw McpServerException("Error connecting to MCP server: ${e.message}", e)
        }
    }

    override suspend fun getTool(name: String): Executable? {
        val tools = listTools()
        return tools.find { it.name == name }
    }

    override suspend fun callTool(name: String, args: Map<String, String>): McpToolResult {
        try {
            val params = buildJsonObject {
                put("name", JsonPrimitive(name))
                put("arguments", buildJsonObject {
                    args.forEach { (key, value) ->
                        put(key, JsonPrimitive(value))
                    }
                })
            }
            
            val result = sendJsonRpcRequest("tools/call", params)
            
            // MCP spec returns content array, convert to McpToolResult format
            val resultObj = result.jsonObject
            val isError = resultObj["isError"]?.jsonPrimitive?.booleanOrNull ?: false
            val contentArray = resultObj["content"]?.jsonArray
            val structuredContent = resultObj["structuredContent"]
            
            return if (isError && contentArray != null && contentArray.isNotEmpty()) {
                // Extract error message from first content item
                val errorText = contentArray.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
                McpToolResult(name, null, errorText)
            } else {
                // Use structured content if available, otherwise extract from content array
                val output = structuredContent ?: contentArray?.firstOrNull()?.jsonObject?.get("text")
                McpToolResult(name, output, null)
            }
        } catch (e: McpServerException) {
            throw e
        } catch (e: Exception) {
            throw McpServerException("Error calling tool on MCP server: ${e.message}", e)
        }
    }

    override suspend fun listResources(): List<McpResource> {
        try {
            val response = httpClient.get("$baseUrl/resources/list")
            if (response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                return objectMapper.readValue<List<McpResource>>(responseBody)
            } else {
                throw McpServerException("Failed to list resources: ${response.status}")
            }
        } catch (e: Exception) {
            throw McpServerException("Error listing resources from MCP server: ${e.message}", e)
        }
    }

    override suspend fun listResourceTemplates(): List<McpResourceTemplate> {
        try {
            val response = httpClient.get("$baseUrl/resources/templates/list")
            if (response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                return objectMapper.readValue<List<McpResourceTemplate>>(responseBody)
            } else {
                throw McpServerException("Failed to list resource templates: ${response.status}")
            }
        } catch (e: Exception) {
            throw McpServerException("Error listing resource templates from MCP server: ${e.message}", e)
        }
    }

    override suspend fun readResource(uri: String): McpReadResourceResponse {
        try {
            val response = httpClient.post("$baseUrl/resources/read") {
                contentType(ContentType.Application.Json)
                setBody(objectMapper.writeValueAsString(mapOf("uri" to uri)))
            }
            
            if (response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                return objectMapper.readValue<McpReadResourceResponse>(responseBody)
            } else {
                throw McpServerException("Failed to read resource '$uri': ${response.status}")
            }
        } catch (e: Exception) {
            throw McpServerException("Error reading resource from MCP server: ${e.message}", e)
        }
    }

    override suspend fun close() {
        httpClient.close()
    }
}