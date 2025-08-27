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

import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.serialization.Serializable
import tri.ai.core.MChatMessagePart
import tri.ai.core.MChatRole
import tri.ai.core.MPartType
import tri.ai.core.MultimodalChatMessage
import tri.ai.mcp.tool.ToolLibrary
import tri.ai.prompt.PromptLibrary
import tri.ai.prompt.fill

/**
 * Implements basic functionality of MCP prompt server based on a local prompt library.
 */
class LocalMcpServer(val prompts: PromptLibrary = PromptLibrary(), val tools: ToolLibrary) :
    McpServerAdapter, ToolLibrary by tools {

    override fun toString() = "LocalMcpServer"

    /** List prompt information. */
    override suspend fun listPrompts(): List<McpPrompt> =
        prompts.list().map { it.toMcpContract() }

    /** Gets result of filling a prompt with arguments. */
    override suspend fun getPrompt(name: String, args: Map<String, String>): McpGetPromptResponse {
        val prompt = prompts.get(name)
            ?: throw McpServerException("Prompt with name '$name' not found")
        val filled = prompt.fill(args)
        return McpGetPromptResponse(
            description = prompt.description ?: prompt.title(),
            listOf(MultimodalChatMessage(
                role = MChatRole.User,
                content = listOf(MChatMessagePart(MPartType.TEXT, text = filled))
        )))
    }

    override suspend fun getCapabilities() = McpServerCapabilities(
        prompts = McpServerCapability(listChanged = false),
        tools = if (tools.listTools().isEmpty()) null else McpServerCapability(listChanged = false)
    )

    override suspend fun close() {
        // Nothing to close for local adapter
    }

}

/** Response returned from a prompt request. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Serializable
data class McpGetPromptResponse(
    val description: String? = null,
    val messages: List<MultimodalChatMessage>
)