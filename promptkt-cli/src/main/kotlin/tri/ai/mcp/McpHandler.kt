package tri.ai.mcp

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import tri.ai.core.MChatMessagePart
import tri.ai.core.MChatRole

/**
 * Handles MCP-specific business logic for JSON-RPC requests.
 * Focuses on MCP protocol implementation without JSON-RPC concerns.
 */
class McpHandler(private val server: LocalMcpServer) : JsonRpcHandler {

    override suspend fun handleRequest(method: String?, params: JsonObject?): JsonElement? {
        return when (method) {
            // --- Required handshake (advertise prompts capability only) ---
            "initialize" -> handleInitialize()

            // --- Prompts surfaces ---
            "prompts/list" -> handlePromptsList()
            "prompts/get" -> handlePromptsGet(params)

            // --- Optional surfaces with empty responses ---
            "tools/list" -> handleToolsList()
            "tools/call" -> handleToolsCall()
            "resources/list" -> handleResourcesList()
            "resources/read" -> handleResourcesRead()

            // --- Notifications with no responses ---
            "notifications/initialized" -> handleNotificationsInitialized()

            // --- Graceful shutdown notification ---
            "notifications/close" -> handleNotificationsClose()

            // Unknown/unsupported method
            else -> null
        }
    }

    private suspend fun handleInitialize(): JsonElement {
        val capabilities = server.getCapabilities()
        return buildJsonObject {
            put("protocolVersion", "2025-06-18")
            put("serverInfo", buildJsonObject {
                put("name", "promptfx-prompts")
                put("version", "0.1.0")
            })
            put("capabilities", JsonRpcSerializer.toJsonElement(capabilities))
        }
    }

    private suspend fun handlePromptsList(): JsonElement {
        val prompts = server.listPrompts()
        return buildJsonObject {
            put("prompts", JsonRpcSerializer.toJsonElement(prompts))
        }
    }

    private suspend fun handlePromptsGet(params: JsonObject?): JsonElement {
        val name = params?.get("name")?.jsonPrimitive?.content
        if (name.isNullOrBlank()) {
            throw IllegalArgumentException("Invalid params: 'name' is required")
        }
        val argsMap = params["arguments"]?.jsonObject?.let { JsonRpcSerializer.toStringMap(it) } ?: emptyMap()
        return runBlocking {
            server.getPrompt(name, argsMap).toJsonElement()
        }
    }

    private suspend fun handleToolsList() = objwithemptylist("tools")
    private suspend fun handleToolsCall() = objwithemptylist("content")
    private suspend fun handleResourcesList() = objwithemptylist("resources")
    private suspend fun handleResourcesRead() = objwithemptylist("contents")
    private suspend fun handleNotificationsInitialized() = null
    private suspend fun handleNotificationsClose(): JsonElement? {
        // No response for notifications, caller should exit
        runCatching { server.close() }
        return null
    }

    private fun objwithemptylist(id: String) = buildJsonObject { put(id, buildJsonArray { }) }

    /** Convert McpPrompt to JsonElement */
    private fun McpGetPromptResponse.toJsonElement() =
        convertPromptResponse(this)

    //region Response Conversion Logic

    companion object {
        /** Convert McpPrompt to JsonElement */
        fun convertPromptResponse(response: McpGetPromptResponse): JsonElement = buildJsonObject {
            response.description?.let { put("description", JsonPrimitive(it)) }
            put("messages", buildJsonArray {
                for (msg in response.messages) {
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

                // You don't have a resource URI field;
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
 degrade to text so clients don't reject it.
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

    //endregion
}
