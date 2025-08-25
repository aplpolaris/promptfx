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
package tri.ai.pips.api

import com.fasterxml.jackson.databind.JsonNode
import tri.ai.core.TextCompletion
import tri.ai.prompt.PromptDefSchema
import tri.ai.tool.wf.MAPPER

/** Executes text completion for simple text input/output using a completion service. */
class PTextCompletionExecutable(val completion: TextCompletion): PExecutable {
    override val name = "completion/${completion.modelId}"
    override val version = "0.0.0"
    override val inputSchema: JsonNode? = PromptDefSchema.generateChatInputSchema()
    override val outputSchema: JsonNode? = generateTextCompletionOutputSchema()

    override suspend fun execute(input: JsonNode, context: PExecContext): JsonNode {
        val result = completion.complete(input.extractText())
        return MAPPER.createObjectNode().put("text", result.firstValue)
    }

    private fun JsonNode.extractText(): String = when {
        isTextual -> asText()
        has("message") -> get("message").extractText()
        has("text") -> get("text").extractText()
        else -> toString()
    }
    
    private fun generateTextCompletionOutputSchema(): JsonNode {
        val schema = MAPPER.createObjectNode()
        schema.put("\$schema", "http://json-schema.org/draft-07/schema#")
        schema.put("type", "object")
        
        val properties = MAPPER.createObjectNode()
        val textProp = MAPPER.createObjectNode()
        textProp.put("type", "string")
        textProp.put("description", "The completed text from the completion model")
        properties.set<JsonNode>("text", textProp)
        
        val required = MAPPER.createArrayNode()
        required.add("text")
        
        schema.set<JsonNode>("properties", properties)
        schema.set<JsonNode>("required", required)
        
        return schema
    }
}