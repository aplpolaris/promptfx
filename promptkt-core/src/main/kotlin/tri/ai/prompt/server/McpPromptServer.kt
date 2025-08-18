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
package tri.ai.prompt.server

import com.fasterxml.jackson.annotation.JsonInclude
import tri.ai.core.MChatMessagePart
import tri.ai.core.MChatRole
import tri.ai.core.MPartType
import tri.ai.core.MultimodalChatMessage
import tri.ai.prompt.PromptLibrary
import tri.ai.prompt.fill

/**
 * Implements basic functionality of MCP prompt server.
 * @see https://modelcontextprotocol.io/specification/2025-06-18/server/prompts
 */
class McpPromptServer {

    var library = PromptLibrary()

    /** List prompt information. */
    fun listPrompts(): List<McpPrompt> = library.list().map { it.toMcpContract() }

    /** Gets result of filling a prompt with arguments. */
    fun getPrompt(name: String, args: Map<String, String>): McpGetPromptResponse {
        val prompt = library.get(name)
            ?: throw McpServerException("Prompt with name '$name' not found")
        val filled = prompt.fill(args)
        return McpGetPromptResponse(
            description = prompt.description,
            listOf(MultimodalChatMessage(
                role = MChatRole.User,
                content = listOf(MChatMessagePart(MPartType.TEXT, filled))
        )))
    }

}

/** Response returned from a prompt request. */
@JsonInclude(JsonInclude.Include.NON_NULL)
class McpGetPromptResponse(
    val description: String? = null,
    val messages: List<MultimodalChatMessage>
)