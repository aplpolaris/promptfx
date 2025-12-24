package tri.ai.mcp.http

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.annotations.Beta
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import tri.ai.mcp.JsonSerializers
import tri.ai.mcp.JsonSerializers.toJsonElement
import tri.ai.mcp.McpGetPromptResponse
import tri.ai.mcp.McpPrompt
import tri.ai.mcp.McpReadResourceResponse
import tri.ai.mcp.McpResource
import tri.ai.mcp.McpResourceTemplate
import tri.ai.mcp.McpServerAdapter
import tri.ai.mcp.McpServerCapabilities
import tri.ai.mcp.McpServerException
import tri.ai.mcp.tool.McpToolMetadata
import tri.ai.mcp.tool.McpToolResult
import tri.util.fine
import tri.util.info
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
    private val objectMapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(KotlinModule.Builder().build())

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
            return objectMapper.readValue(JsonSerializers.serialize(capabilitiesJson))
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
            return objectMapper.readValue(JsonSerializers.serialize(promptsJson))
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

            return objectMapper.readValue(JsonSerializers.serialize(resultWithCapitalizedRoles))
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

    override suspend fun listTools(): List<McpToolMetadata> {
        try {
            val result = sendJsonRpcRequest("tools/list")
            fine<McpServerAdapterHttp>(result.toString(), null)
            val toolsJson = result.jsonObject["tools"]?.jsonArray
                ?: throw McpServerException("No tools in response")
            return toolsJson.map { objectMapper.readValue<McpToolMetadata>(it.toString()) }
        } catch (e: McpServerException) {
            throw e
        } catch (e: Exception) {
            throw McpServerException("Error connecting to MCP server: ${e.message}", e)
        }
    }

    override suspend fun getTool(name: String): McpToolMetadata? {
        val tools = listTools()
        return tools.find { it.name == name }
    }

    override suspend fun callTool(name: String, args: Map<String, Any?>): McpToolResult {
        try {
            val params = buildJsonObject {
                put("name", JsonPrimitive(name))
                put("arguments", buildJsonObject {
                    args.forEach { (key, value) ->
                        put(key, (value ?: JsonNull).toJsonElement())
                    }
                })
            }

            val result = sendJsonRpcRequest("tools/call", params)
            info<McpServerAdapterHttp>(result.toString())
            return objectMapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue<McpToolResult>(JsonSerializers.serialize(result))
        } catch (e: McpServerException) {
            throw e
        } catch (e: Exception) {
            throw McpServerException("Error calling tool on MCP server: ${e.message}", e)
        }
    }

    override suspend fun listResources(): List<McpResource> {
        try {
            val result = sendJsonRpcRequest("resources/list")
            val resourcesJson = result.jsonObject["resources"]?.jsonArray
                ?: throw McpServerException("No resources in response")

            return objectMapper.readValue(JsonSerializers.serialize(resourcesJson))
        } catch (e: McpServerException) {
            throw e
        } catch (e: Exception) {
            throw McpServerException("Error listing resources from MCP server: ${e.message}", e)
        }
    }

    override suspend fun listResourceTemplates(): List<McpResourceTemplate> {
        try {
            val result = sendJsonRpcRequest("resources/templates/list")
            val templatesJson = result.jsonObject["resourceTemplates"]?.jsonArray
                ?: throw McpServerException("No resource templates in response")

            return objectMapper.readValue(JsonSerializers.serialize(templatesJson))
        } catch (e: McpServerException) {
            throw e
        } catch (e: Exception) {
            throw McpServerException("Error listing resource templates from MCP server: ${e.message}", e)
        }
    }

    override suspend fun readResource(uri: String): McpReadResourceResponse {
        try {
            val params = buildJsonObject {
                put("uri", JsonPrimitive(uri))
            }

            val result = sendJsonRpcRequest("resources/read", params)

            return objectMapper.readValue(JsonSerializers.serialize(result))
        } catch (e: McpServerException) {
            throw e
        } catch (e: Exception) {
            throw McpServerException("Error reading resource from MCP server: ${e.message}", e)
        }
    }

    override suspend fun close() {
        httpClient.close()
    }
}