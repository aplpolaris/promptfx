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
package tri.ai.core.tool.impl

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.Executable
import tri.ai.prompt.PromptDef

/** Fills text into a prompt template, and generates a response using an LLM. */
class PromptChatExecutable(private val def: PromptDef, private val chatExec: TextChat): Executable {

    override val name: String
        get() = "prompt-chat/${def.bareId}"
    override val description: String
        get() = def.description ?: "Prompt template chat ${def.bareId}"
    override val version: String
        get() = def.version ?: "0.0.0"
    override val inputSchema: JsonNode? = null
    override val outputSchema: JsonNode? = null

    @Suppress("UNCHECKED_CAST")
    override suspend fun execute(input: JsonNode, ctx: ExecContext): JsonNode {
        val filled = PromptFillExecutable(def).execute(input, ctx)
        val response = chatExec.chat(listOf(TextChatMessage.Companion.user(filled.asText())))
            .firstValue.textContent()
        return TextNode.valueOf(response)
    }

}
