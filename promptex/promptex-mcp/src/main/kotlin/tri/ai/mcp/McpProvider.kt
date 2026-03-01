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

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
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
import tri.ai.mcp.JsonSerializers.toJsonElement
import tri.ai.mcp.McpJsonRpcHandler.Companion.METHOD_INITIALIZE
import tri.ai.mcp.McpJsonRpcHandler.Companion.METHOD_NOTIFICATIONS_INITIALIZED
import tri.ai.mcp.McpJsonRpcHandler.Companion.METHOD_PROMPTS_GET
import tri.ai.mcp.McpJsonRpcHandler.Companion.METHOD_PROMPTS_LIST
import tri.ai.mcp.McpJsonRpcHandler.Companion.METHOD_RESOURCES_LIST
import tri.ai.mcp.McpJsonRpcHandler.Companion.METHOD_RESOURCES_READ
import tri.ai.mcp.McpJsonRpcHandler.Companion.METHOD_RESOURCES_TEMPLATES_LIST
import tri.ai.mcp.McpJsonRpcHandler.Companion.METHOD_TOOLS_CALL
import tri.ai.mcp.McpJsonRpcHandler.Companion.METHOD_TOOLS_LIST
import tri.ai.mcp.McpJsonRpcHandler.Companion.PROMPTFX_CLIENT_PARAMS
import tri.ai.mcp.tool.McpToolLibrary
import tri.ai.mcp.tool.McpToolMetadata
import tri.ai.mcp.tool.McpToolResponse
import tri.util.fine
import tri.util.info
import tri.util.warning
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.forEach

/**
 * Functionality of an MCP provider (server). Implementations may proxy a remote MCP server, or may be instantiated locally.
 * This library uses the term "provider" to indicate any class that provides MCP services, whether local or remote.
 * The term "server" is reserved for runnable classes that set up a server process according to the MCP specification.
 *
 * @see https://modelcontextprotocol.io/specification/2025-06-18/server/prompts
 * @see https://modelcontextprotocol.io/specification/2025-06-18/server/resources
 */
interface McpProvider: McpToolLibrary {

    /** Initializes the provider. */
    suspend fun initialize()

    /** Get server capabilities. */
    suspend fun getCapabilities(): McpCapabilities?

    /** List all available prompts from the server. */
    suspend fun listPrompts(): List<McpPrompt>
    /** Get a filled prompt with the given arguments. */
    suspend fun getPrompt(name: String, args: Map<String, String> = emptyMap()): McpPromptResponse

    /** List all available resources from the server. */
    suspend fun listResources(): List<McpResource>
    /** List all available resource templates from the server. */
    suspend fun listResourceTemplates(): List<McpResourceTemplate>
    /** Read a resource by URI. */
    suspend fun readResource(uri: String): McpResourceResponse

    /** Close the connection to the server. */
    suspend fun close()
}

/** Partial implementation of [McpProvider] with generic methods for method execution. */
abstract class McpProviderSupport: McpProvider {

    protected val requestId = AtomicInteger(0)
    private val objectMapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(KotlinModule.Builder().build())

    private val initialized = AtomicBoolean(false)
    private var capabilities: McpCapabilities? = null

    //region INTERFACE IMPLEMENTATION

    /** Perform initialization handshake, if not already done. Synchronize so this can only be called once. */
    override suspend fun initialize() {
        if (initialized.get()) return

        synchronized(initialized) {
            if (initialized.get()) return

            try {
                val result = runBlocking {
                    sendJsonRpc(METHOD_INITIALIZE, PROMPTFX_CLIENT_PARAMS)
                }
                fine<McpProvider>("Initialized MCP provider with response: $result")
                capabilities = result.jsonObject["capabilities"]?.jsonObject?.let {
                    objectMapper.readValue(JsonSerializers.serialize(it))
                }
                runBlocking {
                    sendJsonRpc(METHOD_NOTIFICATIONS_INITIALIZED)
                }
                initialized.set(true)
            } catch (e: Exception) {
                throw McpException("Error initializing MCP provider: ${e.message}", e)
            }
        }
    }

    override suspend fun getCapabilities(): McpCapabilities? {
        initialize()
        return capabilities
    }

    override suspend fun listPrompts(): List<McpPrompt> {
        try {
            val result = sendJsonRpcWithInitializationCheck(METHOD_PROMPTS_LIST)
            val promptsJson = result.jsonObject["prompts"]?.jsonArray
                ?: throw McpException("No prompts in response")
            return objectMapper.readValue(JsonSerializers.serialize(promptsJson))
        } catch (e: McpException) {
            throw e
        } catch (e: Exception) {
            throw McpException("Error connecting to MCP server: ${e.message}")
        }
    }

    override suspend fun getPrompt(name: String, args: Map<String, String>): McpPromptResponse {
        try {
            val params = buildJsonObject {
                put("name", JsonPrimitive(name))
                put("arguments", buildJsonObject {
                    args.forEach { (key, value) ->
                        put(key, JsonPrimitive(value))
                    }
                })
            }
            val result = sendJsonRpcWithInitializationCheck(METHOD_PROMPTS_GET, params)
            return objectMapper.readValue(JsonSerializers.serialize(result.withCapitalizedRoles()))
        } catch (e: McpException) {
            throw e
        } catch (e: Exception) {
            throw McpException("Error getting prompt from MCP server: ${e.message}")
        }
    }

    override suspend fun listTools(): List<McpToolMetadata> {
        var result: JsonElement? = null
        try {
            result = sendJsonRpcWithInitializationCheck(METHOD_TOOLS_LIST)
            fine<McpProvider>(result.toString(), null)
            val toolsJson = result.jsonObject["tools"]?.jsonArray
                ?: throw McpException("No tools in response")
            return toolsJson.map { objectMapper.readValue<McpToolMetadata>(it.toString()) }
        } catch (e: McpException) {
            info<McpProvider>(result?.toString() ?: "unexpected result")
            throw e
        } catch (e: Exception) {
            info<McpProvider>(result?.toString() ?: "unexpected result")
            throw McpException("Error connecting to MCP server: ${e.message}", e)
        }
    }

    override suspend fun getTool(name: String): McpToolMetadata? {
        val tools = listTools()
        return tools.find { it.name == name }
    }

    override suspend fun callTool(name: String, args: Map<String, Any?>): McpToolResponse {
        try {
            val params = buildJsonObject {
                put("name", JsonPrimitive(name))
                put("arguments", buildJsonObject {
                    args.forEach { (key, value) ->
                        put(key, (value ?: JsonNull).toJsonElement())
                    }
                })
            }

            val result = sendJsonRpcWithInitializationCheck(METHOD_TOOLS_CALL, params)
            fine<McpProvider>(result.toString())
            return objectMapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue<McpToolResponse>(JsonSerializers.serialize(result))
        } catch (e: McpException) {
            throw e
        } catch (e: Exception) {
            throw McpException("Error calling tool on MCP server: ${e.message}", e)
        }
    }

    override suspend fun listResources(): List<McpResource> {
        try {
            val result = sendJsonRpcWithInitializationCheck(METHOD_RESOURCES_LIST)
            val resourcesJson = result.jsonObject["resources"]?.jsonArray
                ?: throw McpException("No resources in response")

            return objectMapper.readValue(JsonSerializers.serialize(resourcesJson))
        } catch (e: McpException) {
            throw e
        } catch (e: Exception) {
            throw McpException("Error listing resources from MCP server: ${e.message}", e)
        }
    }

    override suspend fun listResourceTemplates(): List<McpResourceTemplate> {
        try {
            val result = sendJsonRpcWithInitializationCheck(METHOD_RESOURCES_TEMPLATES_LIST)
            val templatesJson = result.jsonObject["resourceTemplates"]?.jsonArray
                ?: throw McpException("No resource templates in response")

            return objectMapper.readValue(JsonSerializers.serialize(templatesJson))
        } catch (e: McpException) {
            throw e
        } catch (e: Exception) {
            throw McpException("Error listing resource templates from MCP server: ${e.message}", e)
        }
    }

    override suspend fun readResource(uri: String): McpResourceResponse {
        try {
            val params = buildJsonObject {
                put("uri", JsonPrimitive(uri))
            }

            val result = sendJsonRpcWithInitializationCheck(METHOD_RESOURCES_READ, params)

            return objectMapper.readValue(JsonSerializers.serialize(result))
        } catch (e: McpException) {
            throw e
        } catch (e: Exception) {
            throw McpException("Error reading resource from MCP server: ${e.message}", e)
        }
    }

    //endregion

    /** Send a JSON-RPC request to the MCP server at /mcp endpoint. */
    protected abstract suspend fun sendJsonRpcWithInitializationCheck(method: String, params: JsonObject? = null): JsonElement

    /** Send a JSON-RPC request to the MCP server at /mcp endpoint, without trying to initialize. */
    protected abstract suspend fun sendJsonRpc(method: String, params: JsonObject? = null): JsonElement

    //region INTERNAL UTILS

    /**
     * Convert lowercase roles from MCP spec ("user", "assistant") to capitalized form ("User", "Assistant")
     * and convert content from single object to array format needed by internal MChatRole enum
     */
    protected fun JsonElement.withCapitalizedRoles(): JsonElement {
        if (this !is JsonObject) return this

        val messages = this["messages"]?.jsonArray ?: return this
        val updatedMessages = buildJsonArray {
            for (message in messages) {
                if (message is JsonObject) {
                    val role = message["role"]?.jsonPrimitive?.content
                    val content = message["content"]
                    addJsonObject {
                        message.forEach { (key, value) ->
                            when (key) {
                                "role" -> put(key, JsonPrimitive(role?.replaceFirstChar { it.uppercase() } ?: "User"))
                                "content" -> {
                                    // Convert single content object to array if needed
                                    when (content) {
                                        is JsonObject -> put(key, buildJsonArray { add(convertMcpContentToInternal(content)) })
                                        is JsonArray -> put(key, buildJsonArray { content.forEach { add(convertMcpContentToInternal(it)) } })
                                        else -> put(key, value)
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
            forEach { (key, value) ->
                when (key) {
                    "messages" -> put(key, updatedMessages)
                    else -> put(key, value)
                }
            }
        }
    }

    /** Convert MCP content format to internal format. */
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

    //endregion
}
