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
import tri.ai.mcp.tool.McpToolLibrary
import tri.ai.prompt.PromptLibrary
import tri.ai.prompt.fill

/**
 * MCP provider using in-memory prompt and tool libraries.
 */
class McpProviderEmbedded(
    val prompts: PromptLibrary = PromptLibrary(),
    val tools: McpToolLibrary,
    private val resources: List<McpResource> = emptyList()
) : McpProvider, McpToolLibrary by tools {

    override fun toString() = "McpServer-Embedded"

    override suspend fun initialize() { }

    /** List prompt information. */
    override suspend fun listPrompts(): List<McpPrompt> =
        prompts.list().map { it.toMcpContract() }

    /** Gets result of filling a prompt with arguments. */
    override suspend fun getPrompt(name: String, args: Map<String, String>): McpPromptResponse {
        val prompt = prompts.get(name)
            ?: throw McpException("Prompt with name '$name' not found")
        val filled = prompt.fill(args)
        return McpPromptResponse(
            description = prompt.description ?: prompt.title(),
            listOf(MultimodalChatMessage(
                role = MChatRole.User,
                content = listOf(MChatMessagePart(MPartType.TEXT, text = filled))
        )))
    }

    override suspend fun listResources(): List<McpResource> = resources

    override suspend fun listResourceTemplates(): List<McpResourceTemplate> = emptyList()

    override suspend fun readResource(uri: String): McpResourceResponse {
        val resource = resources.find { it.uri == uri }
            ?: throw McpException("Resource with URI '$uri' not found")
        
        // For sample resources, generate simple content based on the URI
        val content = when {
            uri.startsWith("file://") -> {
                val filename = uri.substringAfterLast("/")
                "Sample content for $filename\n\nThis is a test resource provided by the embedded MCP server."
            }
            uri.startsWith("data://") -> {
                "Sample data resource: ${resource.description ?: resource.name}"
            }
            else -> "Content for ${resource.name}"
        }
        
        return McpResourceResponse(
            contents = listOf(
                McpResourceContents(
                    uri = uri,
                    mimeType = resource.mimeType,
                    text = content
                )
            )
        )
    }

    override suspend fun getCapabilities() = McpCapabilities(
        prompts = McpCapability(listChanged = false),
        tools = if (tools.listTools().isEmpty()) null else McpCapability(listChanged = false),
        resources = if (resources.isEmpty()) null else McpCapability(listChanged = false)
    )

    override suspend fun close() {
        // Nothing to close for embedded provider
    }

}

/** Response returned from a prompt request. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Serializable
data class McpPromptResponse(
    val description: String? = null,
    val messages: List<MultimodalChatMessage>
)