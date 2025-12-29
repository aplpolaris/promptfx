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

import kotlinx.serialization.json.*
import tri.ai.mcp.McpJsonRpcHandler.Companion.METHOD_NOTIFICATIONS_PREFIX
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Remote MCP provider that connects to external MCP servers via stdio.
 * Launches an external process and communicates with it using JSON-RPC over stdio.
 */
class McpProviderStdio(
    private val command: String,
    private val args: List<String> = emptyList(),
    private val env: Map<String, String> = emptyMap()
) : McpProviderSupport() {
    
    private var process: Process? = null
    private var writer: OutputStreamWriter? = null
    private var reader: BufferedReader? = null

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

    /** Send a JSON-RPC request to the MCP server at /mcp endpoint. */
    override suspend fun sendJsonRpcRequest(method: String, params: JsonObject?): JsonElement {
        initialize()
        return sendJsonRpcInitialization(method, params)
    }

    /** Send a JSON-RPC request to the MCP server at /mcp endpoint, without trying to initialize. */
    override suspend fun sendJsonRpcInitialization(method: String, params: JsonObject?): JsonElement {
        if (process == null)
            startProcess()
        val request = buildJsonObject {
            put("jsonrpc", JsonPrimitive("2.0"))
            if (!method.startsWith(METHOD_NOTIFICATIONS_PREFIX))
                put("id", JsonPrimitive(requestId.getAndIncrement()))
            put("method", JsonPrimitive(method))
            if (params != null) {
                put("params", params)
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
