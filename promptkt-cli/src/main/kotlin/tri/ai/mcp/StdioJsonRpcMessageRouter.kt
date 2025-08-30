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

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintStream

/**
 * Handles JSON-RPC 2.0 message routing and protocol-level concerns.
 * Separates message parsing, routing, and response writing from business logic.
 */
class StdioJsonRpcMessageRouter(private val handler: JsonRpcHandler) {

    /** Start a blocking stdio loop reading JSON-RPC requests and writing responses. */
    suspend fun startServer(stream: InputStream, out: PrintStream) {
        val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))

        while (true) {
            val line = reader.readLine() ?: break
            if (line.isBlank()) continue

            val req = runCatching { JsonRpcSerializer.parseRequest(line) }.getOrElse {
                // Malformed JSON → JSON-RPC parse error (-32700)
                writeError(out, null, -32700, "Parse error")
                null
            }
            if (req == null) continue

            val id = req["id"]
            val method = req["method"]?.jsonPrimitive?.contentOrNull
            val params = req["params"]?.jsonObject

            try {
                val result = handler.handleRequest(method, params)
                if (result != null) {
                    writeResult(out, id, result)
                } else if (method == "notifications/close") {
                    // Special case: close notification should exit
                    return
                } else if (method?.startsWith("notifications/") == true) {
                    // Other notifications have no response
                } else {
                    writeError(out, id, -32601, "Method not found: $method")
                }
            } catch (t: Throwable) {
                writeError(out, id, -32603, "Internal error: ${t.message ?: t::class.simpleName}")
            }
        }
    }

    private fun writeResult(out: PrintStream, id: JsonElement?, result: JsonElement) {
        val resp = buildJsonObject {
            put("jsonrpc", JsonPrimitive("2.0"))
            if (id != null) put("id", id)
            put("result", result)
        }
        out.println(JsonRpcSerializer.serialize(resp))
        out.flush()
    }

    private fun writeError(out: PrintStream, id: JsonElement?, code: Int, message: String) {
        val err = buildJsonObject {
            put("jsonrpc", JsonPrimitive("2.0"))
            if (id != null) put("id", id)
            put("error", buildJsonObject {
                put("code", JsonPrimitive(code))
                put("message", JsonPrimitive(message))
            })
        }
        out.println(JsonRpcSerializer.serialize(err))
        out.flush()
    }
}

