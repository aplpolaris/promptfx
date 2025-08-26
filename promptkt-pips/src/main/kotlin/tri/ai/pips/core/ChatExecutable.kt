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
package tri.ai.pips.core

import com.fasterxml.jackson.databind.JsonNode
import tri.ai.core.CompletionBuilder
import tri.ai.core.TextChat

/** Executes for simple text input/output using a chat service. */
class ChatExecutable(val chat: TextChat): Executable {
    override val name = "chat/${chat.modelId}"
    override val description = "Chat using model ${chat.modelId}"
    override val version = "0.0.0"
    override val inputSchema: JsonNode? = null
    override val outputSchema: JsonNode? = null

    override suspend fun execute(input: JsonNode, context: ExecContext): JsonNode {
        val result = CompletionBuilder()
            .text(input.extractText())
            .execute(chat)
        return MAPPER.createObjectNode().put("message", result.firstValue.textContent())
    }

    private fun JsonNode.extractText(): String = when {
        isTextual -> asText()
        has("message") -> get("message").extractText()
        has("text") -> get("text").extractText()
        else -> toString()
    }

}
