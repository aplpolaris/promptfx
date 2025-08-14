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

import tri.ai.prompt.AiPrompt.Companion.fill

/**
 * An MCP specification prompt.
 * @see https://modelcontextprotocol.io/specification/2025-06-18/server/prompts
 */
open class McpPrompt(
    val name: String,
    val title: String? = null,
    val description: String? = null,
    val arguments: List<McpPromptArgument>? = null
)

/** An argument for an MCP prompt. */
class McpPromptArgument(
    val name: String,
    val description: String,
    val required: Boolean
)

/** MCP prompt backed by a mustache template. */
class McpPromptWithTemplate(
    _name: String,
    _title: String,
    _description: String,
    _arguments: List<McpPromptArgument>,
    val template: String
) :
    McpPrompt(_name, _title, _description, _arguments) {

    /** Fills the template with the provided arguments. */
    fun fill(args: Map<String, Any>) = template.fill(args)
}
