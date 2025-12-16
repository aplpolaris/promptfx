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
import tri.ai.core.MChatMessagePart
import tri.ai.core.MChatRole
import tri.ai.mcp.JsonSerializers.serialize
import tri.ai.mcp.JsonSerializers.toJsonElement
import tri.ai.mcp.tool.McpToolResult
import tri.util.json.convertToKotlinxJsonObject

/**
 * Handles MCP-specific business logic for JSON-RPC requests.
 * Focuses on MCP protocol implementation without JSON-RPC concerns.
 */
class McpServerHandler(private val server: McpServerAdapter) : JsonRpcHandler {

    override suspend fun handleRequest(method: String?, params: JsonObject?): JsonElement? {
        return when (method) {
            // --- Required handshake (advertise prompts capability only) ---
            "initialize" -> handleInitialize()

            // --- Prompts surfaces ---
            "prompts/list" -> handlePromptsList()
            "prompts/get" -> handlePromptsGet(params)

            // --- Optional surfaces with empty responses ---
            "tools/list" -> handleToolsList()
            "tools/call" -> handleToolsCall(params)
            "resources/list" -> handleResourcesList()
            "resources/templates/list" -> handleResourceTemplatesList()
            "resources/read" -> handleResourcesRead(params)

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
            capabilities?.let { put("capabilities", JsonSerializers.toJsonElement(it)) }
        }
    }

    private suspend fun handlePromptsList() = buildJsonObject {
        put("prompts", toJsonElement(server.listPrompts()))
    }
    private suspend fun handlePromptsGet(params: JsonObject?): JsonElement {
        val name = params?.get("name")?.jsonPrimitive?.content
        require(!name.isNullOrBlank()) { "Invalid params: 'name' is required" }
        val argsMap = params["arguments"]?.jsonObject?.let { JsonSerializers.toStringMap(it) } ?: emptyMap()
        return convertPromptResponse(server.getPrompt(name, argsMap))
    }

    private suspend fun handleToolsList() = buildJsonObject {
        put("tools", toJsonElement(server.listTools()))
    }
    private suspend fun handleToolsCall(params: JsonObject?): JsonElement {
        val name = params?.get("name")?.jsonPrimitive?.content
        require(!name.isNullOrBlank()) { "Invalid params: 'name' is required" }
        val argsMap = params["arguments"]?.jsonObject?.let { JsonSerializers.toStringMap(it) } ?: emptyMap()
        return convertToolResponse(server.callTool(name, argsMap))
    }

    private suspend fun handleResourcesList() = buildJsonObject {
        put("resources", toJsonElement(server.listResources()))
    }
    private suspend fun handleResourceTemplatesList() = buildJsonObject {
        put("resourceTemplates", toJsonElement(server.listResourceTemplates()))
    }
    private suspend fun handleResourcesRead(params: JsonObject?): JsonElement {
        val uri = params?.get("uri")?.jsonPrimitive?.content
        require(!uri.isNullOrBlank()) { "Invalid params: 'uri' is required" }
        return toJsonElement(server.readResource(uri))
    }
    private suspend fun handleNotificationsInitialized() = null
    private suspend fun handleNotificationsClose(): JsonElement? {
        // No response for notifications, caller should exit
        runCatching { server.close() }
        return null
    }

    private fun objwithemptylist(id: String) = buildJsonObject { put(id, buildJsonArray { }) }

    //region Response Conversion Logic

    companion object {
        /** Convert McpPrompt to JsonElement */
        fun convertPromptResponse(response: McpGetPromptResponse): JsonElement = buildJsonObject {
            response.description?.let { put("description", it) }
            put("messages", buildJsonArray {
                for (msg in response.messages) {
                    val parts = msg.content.orEmpty()
                    when (parts.size) {
                        0 -> addJsonObject {
                            put("role", JsonPrimitive(msg.role.asMcpRole()))
                            put("content", textContent(""))
                        }
                        1 -> addJsonObject {
                            put("role", msg.role.asMcpRole())
                            put("content", parts.first().toMcpContent())
                        }
                        else -> {
                            parts.forEach {
                                addJsonObject {
                                    put("role", msg.role.asMcpRole())
                                    put("content", it.toMcpContent())
                                }
                            }
                        }
                    }
                }
            })
        }

        /** Convert McpToolResponse to JsonElement */
        fun convertToolResponse(response: McpToolResult): JsonElement = buildJsonObject {
            response.isError?.let { error ->
                put("isError", true)
                putJsonArray("content") { add(textContent(error)) }
                return@buildJsonObject
            }
            response.content?.let { output ->
                val element = convertToKotlinxJsonObject(output)
                putJsonArray("content") { add(textContent(serialize(element))) }
                put("structuredContent", element)
            }
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
                "IMAGE" -> buildJsonObject {
                    put("type", "image")
                    put("data", inlineData.orEmpty())
                    put("mimeType", "image/png")
                }
                "AUDIO" -> buildJsonObject {
                    put("type", "audio")
                    put("data", inlineData.orEmpty())
                    put("mimeType", "audio/wav")
                }

                // You don't have a resource URI field, degrade to text so clients don't reject it.
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
            put("type", "text")
            put("text", s)
        }
    }

    //endregion
}
