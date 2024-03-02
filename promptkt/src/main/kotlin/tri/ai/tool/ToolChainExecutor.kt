/*-
 * #%L
 * promptkt-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
import tri.util.ANSI_CYAN
import tri.util.ANSI_GRAY
import tri.util.ANSI_GREEN
import tri.util.ANSI_RESET
import tri.util.ANSI_YELLOW

/** Executes a series of tools using planning operations. */
class ToolChainExecutor(val completionEngine: TextCompletion) {

    val toolPrompt = """
    Answer the following question. You have access to the following tools:

    {{tools}}

    Use the following format:

    Question: the input question you must answer
    Thought: you should always think about what to do
    Action: the action to take, should be one of [{{tool_names}}]
    Action Input: the input to the action
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
    val logPrompts = false
    val iterationLimit = 5

    /** Answers a question while leveraging a series of tools to get to the answer. */
    fun executeChain(question: String, tools: List<Tool>): String {
        println("User Question: $ANSI_YELLOW$question$ANSI_RESET")

        val templateForQuestion = toolPrompt
            .replace("{{tools}}", tools.joinToString("\n") { "${it.name}: ${it.description}" })
            .replace("{{tool_names}}", tools.joinToString(", ") { it.name })
            .replace("{{history}}", "None")
            .replace("{{input}}", question)

        return runBlocking {
            var result = ToolResult("")
            var iterations = 0
            while (result.finalResult == null && iterations++ < iterationLimit) {
                val stepResult = runExecChain(templateForQuestion, result.historyText, tools)
                result = ToolResult(result.historyText + stepResult.historyText, stepResult.finalResult)
            }
            result.finalResult ?: "I was unable to determine a final answer."
        }
    }

    /** Runs a single step of an execution chain. */
    private suspend fun runExecChain(promptTemplate: String, prevResults: String, tools: List<Tool>): ToolResult {
        val prompt = promptTemplate.replace("{{agent_scratchpad}}", prevResults)
        if (logPrompts)
            prompt.lines().forEach { println("$ANSI_GRAY        $it$ANSI_RESET") }

        val textCompletion = completionEngine.complete(prompt, stop = "Observation: ")
            .value!!.trim()
            .replace("\n\n", "\n")
        println("$ANSI_GREEN$textCompletion$ANSI_RESET")

        val responseOp = textCompletion.split("\n")
            .map { it.split(":", limit = 2) }
            .filter { it.size == 2 }
            .associate { it[0] to it[1] }
        responseOp["Final Answer"]?.trim()?.let {
            return ToolResult(textCompletion, it)
        }

        val toolName = responseOp["Action"]?.trim()
        val tool = tools.find { it.name == toolName }!!
        val toolInput = responseOp["Action Input"]?.trim()
        val observation = tool.run(toolInput!!)
        println("Observation: $ANSI_CYAN$observation$ANSI_RESET")

        return ToolResult(
            historyText = "$textCompletion\nObservation: $observation",
            finalResult = if (tool.isTerminal) observation else null
        )
    }

}
