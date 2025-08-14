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
package tri.ai.prompt

import tri.ai.core.MChatMessagePart
import tri.ai.core.MChatRole
import tri.ai.core.MPartType
import tri.ai.core.MultimodalChatMessage

/** Implements basic functionality of MCP prompt server. */
class McpPromptServer {

    private val prompts = mutableListOf<McpPromptWithTemplate>()

    /** List prompt information. */
    fun listPrompts(): List<McpPrompt> = prompts

    /** Gets result of filling a prompt with arguments. */
    fun getPrompt(name: String, args: Map<String, String>): McpGetPromptResponse {
        val prompt = prompts.find { it.name == name }
            ?: throw PromptNotFoundException("Prompt with name '$name' not found")
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
class McpGetPromptResponse(
    val description: String? = null,
    val messages: List<MultimodalChatMessage>
)

/** Exception thrown when a prompt is not found. */
class PromptNotFoundException(message: String) : Exception(message)

//region MCP SERVER CAPABILITIES

class McpServerCapabilities(
    val prompts: McpServerPromptCapability
)

class McpServerPromptCapability(
    val listChanged: Boolean = false
)

//endregion
