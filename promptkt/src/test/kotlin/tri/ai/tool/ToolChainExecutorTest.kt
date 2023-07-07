package tri.ai.tool

import com.aallam.openai.api.logging.LogLevel
import org.junit.jupiter.api.Test
import tri.ai.openai.OpenAiCompletion
import tri.ai.openai.OpenAiCompletionChat
import tri.ai.openai.OpenAiSettings

class ToolChainExecutorTest {
    @Test
    fun testTools() {
        OpenAiSettings.logLevel = LogLevel.None

        val tool1 = object : Tool("Calculator", "Use this to do math") {
            override suspend fun run(input: String) = "42"
        }
        val tool2 = object : Tool("Romanizer", "Converts numbers to Roman numerals") {
            override suspend fun run(input: String) = input.toInt().let {
                when (it) {
                    42 -> "XLII"
                    84 -> "LXXXIV"
                    else -> "I don't know"
                }
            }
        }

        ToolChainExecutor(OpenAiCompletionChat())
            .executeChain("Multiply 21 times 2 and then convert it to Roman numerals.", listOf(tool1, tool2))
    }

    @Test
    fun testTools2() {
        OpenAiSettings.logLevel = LogLevel.None

        val tool1 = object : Tool("Data Query", "Use this to search for data that is needed to answer a question") {
            override suspend fun run(input: String) = OpenAiCompletionChat().complete(input, tokens = 500).value!!
        }
        val tool2 = object : Tool("Timeline", "Use this once you have all the data needed to show the result on a timeline. Provide structured data as input.", isTerminal = true) {
            override suspend fun run(input: String) = OpenAiCompletionChat().complete("""
                Create a JSON object that can be used to plot a timeline of the following information:
                $input
                The result should confirm to the vega-lite spec, using either a Gantt chart or a dot plot.
                Each event, date, or date range should be shown as a separate entry on the y-axis, sorted by date.
                Provide the JSON result only, no explanation.
            """.trimIndent(), tokens = 1000).value!!
        }
        ToolChainExecutor(OpenAiCompletionChat())
            .executeChain("Look up data with the birth years of the first 10 US presidents along with the order of their presidency, and then visualize the results.", listOf(tool1, tool2))
    }

    @Test
    fun testTools3() {
        OpenAiSettings.logLevel = LogLevel.None

        val tool1 = object : Tool("Calculator", "Use this to do math") {
            override suspend fun run(input: String) = "42"
        }
        val tool2 = object : Tool("Romanizer", "Converts numbers to Roman numerals") {
            override suspend fun run(input: String) = input.toInt().let {
                when (it) {
                    42 -> "XLII"
                    84 -> "LXXXIV"
                    else -> "I don't know"
                }
            }
        }

        ToolChainExecutor(OpenAiCompletionChat())
            .executeChain("Multiply 21 times 2 and then convert it to Roman numerals.", listOf(tool1, tool2))
    }

}