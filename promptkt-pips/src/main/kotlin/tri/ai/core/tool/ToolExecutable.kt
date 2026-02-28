/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.core.tool

import com.fasterxml.jackson.databind.JsonNode
import tri.util.json.createResult
import tri.util.json.inputText

/**
 * Base class for tool-like executables that work with simple string input/output.
 * This replaces the deprecated Tool class by implementing Executable directly.
 */
abstract class ToolExecutable(
    override val name: String,
    override val description: String,
    override val version: String = "1.0.0",
    override val inputSchema: JsonNode? = null,
    override val outputSchema: JsonNode? = null
) : Executable {

    override suspend fun execute(input: JsonNode, context: ExecContext): JsonNode {
        val result = run(input.inputText, context)
        return createResult(result = result.result, isTerminal = result.isTerminal)
    }

    /**
     * Execute the tool with string input and context.
     * Returns a ToolResult with the output, terminal status, and optional final result.
     */
    abstract suspend fun run(input: String, context: ExecContext): ToolExecutableResult

}

/**
 * Result of a tool executable execution.
 */
data class ToolExecutableResult(
    val result: String,
    val isTerminal: Boolean = false
)
