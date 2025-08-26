package tri.ai.cli

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import tri.ai.core.MChatMessagePart
import tri.ai.core.MChatRole
import tri.ai.pips.core.MAPPER
import tri.ai.prompt.PromptLibrary
import tri.ai.prompt.server.LocalMcpServer
import tri.ai.prompt.server.McpGetPromptResponse
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintStream

/** Local MCP server running on stdio -- switch out for a library when possible. */
class LocalMcpStdioServer() {

    private val server = LocalMcpServer(PromptLibrary.Companion.INSTANCE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
        prettyPrint = false
    }

    /** Start a blocking stdio loop reading JSON-RPC requests from [stream] and writing responses to [out]. */
    suspend fun startServer(stream: InputStream, out: PrintStream) {
        val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))

        fun writeResult(id: JsonElement?, result: JsonElement) {
            val resp = buildJsonObject {
                put("jsonrpc", JsonPrimitive("2.0"))
                if (id != null) put("id", id)
                put("result", result)
            }
            out.println(json.encodeToString(resp))
            out.flush()
        }

        fun writeError(id: JsonElement?, code: Int, message: String) {
            val err = buildJsonObject {
                put("jsonrpc", JsonPrimitive("2.0"))
                if (id != null) put("id", id)
                put("error", buildJsonObject {
                    put("code", JsonPrimitive(code))
                    put("message", JsonPrimitive(message))
                })
            }
            out.println(json.encodeToString(err))
            out.flush()
        }

        while (true) {
            val line = reader.readLine() ?: break
            if (line.isBlank()) continue

            val req = runCatching { json.parseToJsonElement(line).jsonObject }.getOrElse {
                // Malformed JSON → JSON-RPC parse error (-32700)
                writeError(null, -32700, "Parse error")
                null
            }
            if (req == null) continue

            val id = req["id"]
            val method = req["method"]?.jsonPrimitive?.contentOrNull
            val params = req["params"]?.jsonObject

            try {
                when (method) {
                    // --- Required handshake (advertise prompts capability only) ---
                    "initialize" -> {
                        val server = server.getCapabilities()
                        val resp = buildJsonObject {
                            put("protocolVersion", "2025-06-18")
                            put("serverInfo", buildJsonObject {
                                put("name", "promptfx-prompts")
                                put("version", "0.1.0")
                            })
                            put("capabilities", json.encodeToJsonElement(server))
                        }
                        writeResult(id, resp)
                    }

                    // --- Prompts surfaces ---
                    "prompts/list" -> {
                        val prompts = server.listPrompts()
                        val resp = buildJsonObject {
                            put("prompts", json.encodeToJsonElement(prompts))
                        }
                        writeResult(id, resp)
                    }

                    "prompts/get" -> {
                        val name = params?.get("name")?.jsonPrimitive?.content
                        if (name.isNullOrBlank()) {
                            writeError(id, -32602, "Invalid params: 'name' is required")
                            continue
                        }
                        val argsMap = params["arguments"]?.jsonObject?.toStringMap() ?: emptyMap()
                        val respElem: JsonElement = runBlocking {
                            server.getPrompt(name, argsMap).toJsonElement()
                        }
                        writeResult(id, respElem)
                    }

                    // --- Optional surfaces with empty responses ---
                    "tools/list" -> { writeResult(id, buildJsonObject { put("tools", buildJsonArray { }) }) }
                    "tools/call" -> { writeResult(id, buildJsonObject { put("content", buildJsonArray { }) }) }
                    "resources/list" -> { writeResult(id, buildJsonObject { put("resources", buildJsonArray { }) }) }
                    "resources/read" -> { writeResult(id, buildJsonObject { put("contents", buildJsonArray { }) }) }

                    // --- Notifications with no responses ---
                    "notifications/initialized" -> { }

                    // --- Graceful shutdown notification ---
                    "notifications/close" -> {
                        runCatching { server.close() }
                        // No response for notifications;
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
 exit
                        return
                    }

                    // Unknown/unsupported method
                    else -> writeError(id, -32601, "Method not found: $method")
                }
            } catch (t: Throwable) {
                writeError(id, -32603, "Internal error: ${t.message ?: t::class.simpleName}")
            }
        }
    }

    //region JSONElement CONVERSIONS

    /** Convert any DTO to JsonElement via Jackson -> string -> kotlinx JsonElement */
    private fun Any.toJsonElement(): JsonElement =
        json.parseToJsonElement(MAPPER.writeValueAsString(this))

    /** Convert McpPrompt to JsonElement */
    private fun McpGetPromptResponse.toJsonElement() = buildJsonObject {
        description?.let { put("description", JsonPrimitive(it)) }
        put("messages", buildJsonArray {
            for (msg in messages) {
                val parts = msg.content.orEmpty()
                when (parts.size) {
                    0 -> add(
                        buildJsonObject {
                            put("role", JsonPrimitive(msg.role.asMcpRole()))
                            put("content", buildJsonObject {
                                put("type", JsonPrimitive("text"))
                                put("text", JsonPrimitive(""))
                            })
                        }
                    )

                    1 -> add(
                        buildJsonObject {
                            put("role", JsonPrimitive(msg.role.asMcpRole()))
                            put("content", parts.first().toMcpContent())
                        }
                    )

                    else -> {
                        // Split multiple parts into multiple messages (same role), each with one content object.
                        for (part in parts) {
                            add(
                                buildJsonObject {
                                    put("role", JsonPrimitive(msg.role.asMcpRole()))
                                    put("content", part.toMcpContent())
                                }
                            )
                        }
                    }
                }
            }
        })
    }

    /** Map your chat role enum to MCP's lowercase roles. Adjust as needed for your enum. */
    fun MChatRole.asMcpRole(): String = when (name.lowercase()) {
        "user" -> "user"
        "assistant" -> "assistant"
        // If you have "system" or others, default to "user" for prompts.
        else -> "user"
    }
    /** Convert one of your message parts to a single MCP content object. */
    fun MChatMessagePart.toMcpContent(): JsonObject {
        val kind = partType.name.uppercase()

        return when (kind) {
            "TEXT" -> textContent(text.orEmpty())

            // If you’re embedding base64 data (no mime in your type), provide a sensible default.
            "IMAGE" -> buildJsonObject {
                put("type", JsonPrimitive("image"))
                put("data", JsonPrimitive(inlineData.orEmpty()))
                put("mimeType", JsonPrimitive("image/png"))
            }
            "AUDIO" -> buildJsonObject {
                put("type", JsonPrimitive("audio"))
                put("data", JsonPrimitive(inlineData.orEmpty()))
                put("mimeType", JsonPrimitive("audio/wav"))
            }

            // You don’t have a resource URI field; degrade to text so clients don’t reject it.
            "RESOURCE" -> textContent(text ?: "[resource omitted: no URI/mimeType in part]")

            // If your part encodes a function/tool call, flatten it into text for prompts.
            "FUNCTION", "TOOL", "FUNCTION_CALL" -> {
                val argsStr = functionArgs?.entries
                    ?.joinToString(", ") { (k, v) -> "$k=$v" }
                    ?.let { "($it)" } ?: ""
                textContent("function:${functionName ?: "unknown"}$argsStr")
            }

            else -> textContent(text ?: "[unsupported part: $kind]")
        }
    }

    private fun textContent(s: String) = buildJsonObject {
        put("type", JsonPrimitive("text"))
        put("text", JsonPrimitive(s))
    }

    /** Convert a JsonObject like {"k":"v"} into Map<String,String> */
    private fun JsonObject.toStringMap(): Map<String, String> =
        entries.associate { (k, v) -> k to v.jsonPrimitive.content }

    //endregion

    suspend fun close() {
        server.close()
    }

}
