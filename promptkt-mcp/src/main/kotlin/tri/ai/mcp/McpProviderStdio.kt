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

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.serialization.json.*
import tri.ai.mcp.tool.McpToolMetadata
import tri.ai.mcp.tool.McpToolResponse
import tri.util.fine
import tri.util.info
import tri.util.json.jsonMapper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicInteger

/**
 * Remote MCP provider that connects to external MCP servers via stdio.
 * Launches an external process and communicates with it using JSON-RPC over stdio.
 */
class McpProviderStdio(
    private val command: String,
    private val args: List<String> = emptyList(),
    private val env: Map<String, String> = emptyMap()
) : McpProvider {
    
    private var process: Process? = null
    private var writer: OutputStreamWriter? = null
    private var reader: BufferedReader? = null
    private val requestId = AtomicInteger(0)
    private val objectMapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(KotlinModule.Builder().build())
    
    init {
        startProcess()
    }
    
    private fun startProcess() {
        val processBuilder = ProcessBuilder(listOf(command) + args)
        if (env.isNotEmpty()) {
            processBuilder.environment().putAll(env)
        }
        processBuilder.redirectErrorStream(false)
        
        process = processBuilder.start()
        writer = OutputStreamWriter(process!!.outputStream, Charsets.UTF_8)
        reader = BufferedReader(InputStreamReader(process!!.inputStream, Charsets.UTF_8))
    }
    
    private fun sendRequest(method: String, params: Map<String, Any>? = null): JsonElement {
        val id = requestId.incrementAndGet()
        val request = buildJsonObject {
            put("jsonrpc", JsonPrimitive("2.0"))
            put("id", JsonPrimitive(id))
            put("method", JsonPrimitive(method))
            if (params != null) {
                put("params", Json.parseToJsonElement(objectMapper.writeValueAsString(params)))
            }
        }
        
        synchronized(this) {
            writer?.write(request.toString() + "\n")
            writer?.flush()
            
            // Read response
            val line = reader?.readLine() 
                ?: throw McpException("No response from stdio server")
            
            val response = Json.parseToJsonElement(line).jsonObject
            
            if (response.containsKey("error")) {
                val error = response["error"]?.jsonObject
                val message = error?.get("message")?.jsonPrimitive?.content ?: "Unknown error"
                throw McpException("Stdio server error: $message")
            }
            
            return response["result"] ?: JsonNull
        }
    }

    override suspend fun getCapabilities(): McpCapabilities? {
        try {
            val result = sendRequest("capabilities")
            if (result is JsonNull) return null
            return objectMapper.readValue<McpCapabilities>(result.toString())
        } catch (e: Exception) {
            throw McpException("Error getting capabilities from stdio server: ${e.message}", e)
        }
    }

    override suspend fun listPrompts(): List<McpPrompt> {
        try {
            val result = sendRequest("prompts/list")
            val wrapper = objectMapper.readValue<Map<String, List<McpPrompt>>>(result.toString())
            return wrapper["prompts"] ?: emptyList()
        } catch (e: Exception) {
            throw McpException("Error listing prompts from stdio server: ${e.message}", e)
        }
    }

    override suspend fun getPrompt(name: String, args: Map<String, String>): McpPromptResponse {
        try {
            val result = sendRequest("prompts/get", mapOf("name" to name, "arguments" to args))
            return objectMapper.readValue<McpPromptResponse>(result.toString())
        } catch (e: Exception) {
            throw McpException("Error getting prompt from stdio server: ${e.message}", e)
        }
    }

    override suspend fun listTools(): List<McpToolMetadata> {
        try {
            val result = sendRequest("tools/list")
            fine<McpProviderStdio>(result.toString(), null)
            val toolsJson = result.jsonObject["tools"]?.jsonArray
                ?: throw McpException("No tools in response")
            return toolsJson.map { objectMapper.readValue<McpToolMetadata>(it.toString()) }
        } catch (e: Exception) {
            throw McpException("Error listing tools from stdio server: ${e.message}", e)
        }
    }

    override suspend fun getTool(name: String): McpToolMetadata? {
        val tools = listTools()
        return tools.find { it.name == name }
    }

    override suspend fun callTool(name: String, args: Map<String, Any?>): McpToolResponse {
        try {
            val result = sendRequest("tools/call", mapOf("name" to name, "arguments" to args))
            fine<McpProviderStdio>(result.toString())
            return jsonMapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue<McpToolResponse>(result.toString())
        } catch (e: Exception) {
            throw McpException("Error calling tool on stdio server: ${e.message}", e)
        }
    }

    override suspend fun listResources(): List<McpResource> {
        try {
            val result = sendRequest("resources/list")
            val wrapper = objectMapper.readValue<Map<String, List<McpResource>>>(result.toString())
            return wrapper["resources"] ?: emptyList()
        } catch (e: Exception) {
            throw McpException("Error listing resources from stdio server: ${e.message}", e)
        }
    }

    override suspend fun listResourceTemplates(): List<McpResourceTemplate> {
        try {
            val result = sendRequest("resources/templates/list")
            val wrapper = objectMapper.readValue<Map<String, List<McpResourceTemplate>>>(result.toString())
            return wrapper["resourceTemplates"] ?: emptyList()
        } catch (e: Exception) {
            throw McpException("Error listing resource templates from stdio server: ${e.message}", e)
        }
    }

    override suspend fun readResource(uri: String): McpResourceResponse {
        try {
            val result = sendRequest("resources/read", mapOf("uri" to uri))
            return objectMapper.readValue<McpResourceResponse>(result.toString())
        } catch (e: Exception) {
            throw McpException("Error reading resource from stdio server: ${e.message}", e)
        }
    }

    override suspend fun close() {
        try {
            writer?.close()
            reader?.close()
            process?.destroy()
            process?.waitFor()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}
