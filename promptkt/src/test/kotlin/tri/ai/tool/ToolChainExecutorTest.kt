/*-
 * #%L
 * promptkt-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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

import com.aallam.openai.api.logging.LogLevel
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tri.ai.openai.OpenAiClient
import tri.ai.openai.OpenAiCompletionChat

@Disabled("Requires apikey")
class ToolChainExecutorTest {
    @Test
    fun testTools() {
        OpenAiClient.INSTANCE.settings.logLevel = LogLevel.None

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
        OpenAiClient.INSTANCE.settings.logLevel = LogLevel.None

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
        OpenAiClient.INSTANCE.settings.logLevel = LogLevel.None

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
