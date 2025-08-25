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
import tri.ai.prompt.PromptDef
import tri.ai.prompt.template
import tri.ai.tool.wf.MAPPER

/** Fills text into a prompt template. */
class PPromptExecutable(private val def: PromptDef): PExecutable {

    override val name: String
        get() = "prompt/${def.bareId}"
    override val version: String
        get() = def.version ?: "0.0.0"
    override val inputSchema: JsonNode? = null
    override val outputSchema: JsonNode? = null

    @Suppress("UNCHECKED_CAST")
    override suspend fun execute(input: JsonNode, ctx: PExecContext): JsonNode {
        val args = PPlan.MAPPER.convertValue(input, Map::class.java) as Map<String, Any?>
        val text = def.template().fill(args.filterValues { it != null } as Map<String, Any>)
        return MAPPER.createObjectNode().put("text", text)
    }

}
