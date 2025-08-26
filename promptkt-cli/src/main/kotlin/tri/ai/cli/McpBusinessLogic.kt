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
package tri.ai.cli

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import tri.ai.core.MChatMessagePart
import tri.ai.core.MChatRole
import tri.ai.prompt.server.LocalMcpServer
import tri.ai.prompt.server.McpGetPromptResponse

/**
 * Handles MCP-specific business logic for JSON-RPC requests.
 * Focuses on MCP protocol implementation without JSON-RPC concerns.
 */
class McpBusinessLogic(
    private val server: LocalMcpServer,
    private val serializer: JsonRpcSerializer
) : JsonRpcBusinessLogic {

    override suspend fun handleRequest(method: String?, params: JsonObject?): JsonElement? {
        return when (method) {
            // --- Required handshake (advertise prompts capability only) ---
            "initialize" -> {
                val capabilities = server.getCapabilities()
                buildJsonObject {
                    put("protocolVersion", "2025-06-18")
                    put("serverInfo", buildJsonObject {
                        put("name", "promptfx-prompts")
                        put("version", "0.1.0")
                    })
                    put("capabilities", serializer.toJsonElement(capabilities))
                }
            }

            // --- Prompts surfaces ---
            "prompts/list" -> {
                val prompts = server.listPrompts()
                buildJsonObject {
                    put("prompts", serializer.toJsonElement(prompts))
                }
            }

            "prompts/get" -> {
                val name = params?.get("name")?.jsonPrimitive?.content
                if (name.isNullOrBlank()) {
                    throw IllegalArgumentException("Invalid params: 'name' is required")
                }
                val argsMap = params["arguments"]?.jsonObject?.let { serializer.toStringMap(it) } ?: emptyMap()
                runBlocking {
                    server.getPrompt(name, argsMap).toJsonElement()
                }
            }

            // --- Optional surfaces with empty responses ---
            "tools/list" -> buildJsonObject { put("tools", buildJsonArray { }) }
            "tools/call" -> buildJsonObject { put("content", buildJsonArray { }) }
            "resources/list" -> buildJsonObject { put("resources", buildJsonArray { }) }
            "resources/read" -> buildJsonObject { put("contents", buildJsonArray { }) }

            // --- Notifications with no responses ---
            "notifications/initialized" -> null
            
            // --- Graceful shutdown notification ---
            "notifications/close" -> {
                runCatching { server.close() }
                null // No response for notifications; caller should exit
            }

            // Unknown/unsupported method
            else -> null
        }
    }

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
    private fun MChatRole.asMcpRole(): String = when (name.lowercase()) {
        "user" -> "user"
        "assistant" -> "assistant"
        // If you have "system" or others, default to "user" for prompts.
        else -> "user"
    }
    
    /** Convert one of your message parts to a single MCP content object. */
    private fun MChatMessagePart.toMcpContent(): JsonObject {
        val kind = partType.name.uppercase()

        return when (kind) {
            "TEXT" -> textContent(text.orEmpty())

            // If you're embedding base64 data (no mime in your type), provide a sensible default.
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

            // You don't have a resource URI field; degrade to text so clients don't reject it.
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
}