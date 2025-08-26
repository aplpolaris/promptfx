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
package tri.ai.tool

/** 
 * General purpose functionality that can be leveraged by an LLM agent or prompt.
 * @deprecated Use tri.ai.pips.core.Executable instead. This class will be removed in a future version.
 */
@Deprecated("Use tri.ai.pips.core.Executable instead", ReplaceWith("tri.ai.pips.core.Executable"))
abstract class Tool(
    /** Simple name of tool. */
    val name: String,
    /** Description of tool. */
    val description: String,
    /** Input parameters required by the tool. */
    val requiredParameters: List<String> = listOf(),
    /** Flag indicating if the tool requires an LLM to run. */
    val requiresLlm: Boolean = false,
    /** Flag indicating if the tool is a terminal tool. */
    val isTerminal: Boolean = false
) {
    abstract suspend fun run(input: ToolDict): ToolResult
}

const val TOOL_DICT_INPUT = "input"
const val TOOL_DICT_RESULT = "result"

/** Placeholder for tool inputs and outputs. */
typealias ToolDict = Map<String, String>

val ToolDict.input
    get() = this[TOOL_DICT_INPUT] ?: ""
val ToolDict.result
    get() = this[TOOL_DICT_RESULT] ?: ""

/** Result of a tool execution. */
class ToolResult(val result: ToolDict, val isTerminal: Boolean = false, val finalResult: String? = null) {
    constructor(value: String): this(mapOf(TOOL_DICT_RESULT to value))
    override fun toString() = "ToolResult(result=$result, isTerminal=$isTerminal, finalResult=$finalResult)"
}
