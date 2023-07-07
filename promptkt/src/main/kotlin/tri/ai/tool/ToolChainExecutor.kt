package tri.ai.tool

import kotlinx.coroutines.runBlocking
import tri.ai.core.TextCompletion

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

    companion object {
        private val ANSI_RESET = "\u001B[0m"
        private val ANSI_GREEN = "\u001B[32m"
        private val ANSI_YELLOW = "\u001B[33m"
        private val ANSI_CYAN = "\u001B[36m"
        private val ANSI_GRAY = "\u001B[37m"
    }

}