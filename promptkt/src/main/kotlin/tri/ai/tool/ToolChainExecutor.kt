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

import kotlinx.coroutines.runBlocking
import tri.ai.core.TextCompletion
import tri.util.*

/** Executes a series of tools using planning operations. */
class ToolChainExecutor(val completionEngine: TextCompletion) {

    val logPrompts = false
    val iterationLimit = 5
    val completionTokens = 500

    /** Answers a question while leveraging a series of tools to get to the answer. */
    fun executeChain(question: String, tools: List<Tool>): String {
        info<ToolChainExecutor>("User Question: $ANSI_YELLOW$question$ANSI_RESET")

        val templateForQuestion = TOOL_PROMPT
            .replace("{{tools}}", tools.joinToString("\n") { "${it.name}: ${it.description}" })
            .replace("{{tool_names}}", tools.joinToString(", ") { it.name })
            .replace("{{input}}", question)

        return runBlocking {
            val scratchpad = ToolScratchpad()
            var result: ToolResult? = null
            var iterations = 0
            while (result?.isTerminal != true && iterations++ < iterationLimit) {
                result = runExecChain(templateForQuestion, tools, scratchpad)
            }
            result?.finalResult ?: "I was unable to determine a final answer."
        }
    }

    /** Runs a single step of an execution chain. */
    private suspend fun runExecChain(promptTemplate: String, tools: List<Tool>, scratchpad: ToolScratchpad): ToolResult {
        val prompt = promptTemplate.replace("{{agent_scratchpad}}", scratchpad.summary())
        if (logPrompts)
            prompt.lines().forEach { info<ToolChainExecutor>("$ANSI_GRAY        $it$ANSI_RESET") }

        val textCompletion = completionEngine.complete(prompt, stop = "Observation: ", tokens = completionTokens, history = listOf())
            .firstValue.trim()
            .replace("\n\n", "\n")
        info<ToolChainExecutor>("$ANSI_GREEN$textCompletion$ANSI_RESET")
        scratchpad.steps.add(textCompletion)

        val responseOp = textCompletion.split("\n")
            .map { it.split(":", limit = 2) }
            .filter { it.size == 2 }
            .associate { it[0].trim() to it[1].trim() }
        if ("Final Answer:" in textCompletion) {
            val answer = textCompletion.substringAfter("Final Answer:").trim()
            return ToolResult(mapOf(), isTerminal = true, finalResult = answer)
        }

        val toolName = responseOp["Action"]?.trim()
        val tool = tools.find { it.name == toolName } ?: error("Tool $toolName not found.")

        val toolInput = responseOp["Action Input"]?.trim()
        scratchpad.data[tool.name + " Input"] = toolInput ?: ""
        if (toolInput.isNullOrBlank()) {
            val fallback = "Tool input missing or malformed."
            info<ToolChainExecutor>("Result: $ANSI_RED$fallback$ANSI_RESET")
            scratchpad.data[tool.name + " Result"] = fallback
            return ToolResult(mapOf())
        }

        val toolResult = tool.run(mapOf(TOOL_DICT_INPUT to toolInput))
        info<ToolChainExecutor>("Result: $ANSI_CYAN$toolResult$ANSI_RESET")
        scratchpad.data[tool.name + " Result"] = toolResult.result[TOOL_DICT_RESULT] ?: toolResult.result.toString()
        return toolResult
    }

    companion object {
        val TOOL_PROMPT = """
            Answer the following question. You have access to the following tools:
        
            {{tools}}
        
            Use the following format:
        
            Question: the input question you must answer
            Thought: you should always think about what to do
            Action: the action to take, should be one of [{{tool_names}}]
            Action Input: the input to the action (always provide the full input, including any contextual text provided with the question)
            Observation: the result of the action
            ... (this Thought/Action/Action Input/Observation can repeat N times)
            Thought: I now know the final answer
            Final Answer: the final answer to the original input question
        
            Begin!
        
            Previous conversation history:
            {{history}}
        
            New question: {{input}}
            {{agent_scratchpad}}
        """.trimIndent()
    }

}

/** A scratchpad for storing context, tool results, and steps. */
class ToolScratchpad {
    val data = mutableMapOf<String, String>()
    val steps = mutableListOf<String>()

    fun summary() = data.entries.joinToString("\n") { " - ${it.key}: ${it.value}" }
}