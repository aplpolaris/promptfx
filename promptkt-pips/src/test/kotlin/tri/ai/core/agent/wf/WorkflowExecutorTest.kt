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
package tri.ai.core.agent.wf

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.agent.AgentChatConfig
import tri.ai.core.agent.AgentChatSession
import tri.ai.core.agent.AgentFlowLogger
import tri.ai.openai.OpenAiPlugin

@Tag("openai")
class WorkflowExecutorTest {

    private val GPT35 = OpenAiPlugin().textCompletionModels()[0]
    private val EXEC = WorkflowExecutorChat(AgentChatConfig(GPT35.modelId, maxTokens = 1000, temperature = 0.3))


    //region CALC SOLVERS

    private val CALC_FUNCTION : (String) -> String = { "42" }
    private val ROMANIZER_FUNCTION : (String) -> String = {
        it.toInt().let {
            when (it) {
                42 -> "XLII"
                84 -> "LXXXIV"
                else -> "I don't know"
            }
        }
    }

    private val CALC_SOLVER = RunSolver(
        "Calculator",
        "Use this to do math",
        "Expression to evaluate",
        "Calculated result",
        CALC_FUNCTION
    )
    private val ROMANIZER_SOLVER = RunSolver(
        "Romanizer",
        "Converts numbers to Roman numerals",
        "Positive integer to convert",
        "Roman numeral result",
        ROMANIZER_FUNCTION
    )

    //endregion

    @Test
    fun testWorkflowExecutor_calc() {
        val exec = WorkflowExecutor(EXEC, listOf(CALC_SOLVER, ROMANIZER_SOLVER))
        val problem = MultimodalChatMessage.user("I need a Roman numeral that represents the product 21 times 2.")
        runBlocking {
            exec.sendMessage(AgentChatSession(), problem).events.collect(AgentFlowLogger(verbose = true))
        }
    }

    //region QUERY/TIMELINE SOLVERS

    private val SOLVER_QUERY = ChatSolver(
        "Data Query",
        "Use this to search for data that is needed to answer a question",
        "The query to search for",
        "The result of the query",
        "tools/tool-query"
    )

    private val SOLVER_TIMELINE = ChatSolver(
        "Timeline",
        "Use this once you have all the data needed to visualize the result on a timeline.",
        "The data to visualize",
        "Visualization specification (structured content)",
        "tools/tool-timeline"
    )

    //endregion

    @Test
    fun testWorkflowExecutor_timeline() {
        val exec = WorkflowExecutor(EXEC, listOf(SOLVER_QUERY, SOLVER_TIMELINE))
        val problem = MultimodalChatMessage.user("What is the timeline of the life of Albert Einstein?")
        runBlocking { exec.sendMessage(AgentChatSession(), problem).events.collect(AgentFlowLogger(verbose = true)) }
    }

    @Test
    fun testWorkflowExecutor_timeline2() {
        val exec = WorkflowExecutor(EXEC, listOf(SOLVER_QUERY, SOLVER_TIMELINE))
        val problem = MultimodalChatMessage.user("Give me a timeline visualization of the lifetimes and terms of the first 10 US presidents.")
        runBlocking { exec.sendMessage(AgentChatSession(), problem).events.collect(AgentFlowLogger(verbose = true)) }
    }

    //region SUMMARIZATION SOLVERS

    private val SOLVER_SUMMARY = InstructSolver(
        "Summarization",
        "Use this to summarize a document or article.",
        "The text to summarize",
        "The summary of the text",
        "Generate a 20-30 summary of the text provided below."
    )

    private val SOLVER_KEYDATES = InstructSolver(
        "Key Dates Extraction",
        "Use this to extract key dates from a document or article.",
        "The text to analyze for dates",
        "The extracted key dates",
        "Extract key dates from the text provided below."
    )

    private val SOLVER_TITLEGEN = InstructSolver(
        "Title Generation",
        "Use this to generate a title for a document or article.",
        "The text to analyze for a title",
        "The generated title",
        "Generate a concise and descriptive title for the text provided below."
    )

    //endregion

    @Test
    fun testWorkflowExecutor_article() {
        val exec = WorkflowExecutor(EXEC, listOf(SOLVER_SUMMARY, SOLVER_TITLEGEN, SOLVER_KEYDATES, SOLVER_QUERY, SOLVER_TIMELINE))
        val problem = MultimodalChatMessage.user(
            """
            Provide a quick look on this article, including a short summary, key dates, and a title.

            \"\"\"
            NASA announced on Tuesday that its Artemis II mission, scheduled for launch on November 2024, will be the first crewed lunar flyby in over 50 years. The four astronauts—Reid Wiseman, Victor Glover, Christina Hammock Koch, and Jeremy Hansen—will orbit the Moon without landing before returning to Earth. The mission is a critical step toward Artemis III, which aims to land astronauts on the lunar surface as early as 2026. NASA Administrator Bill Nelson stated that this represents a pivotal moment in humanity’s return to deep space exploration. Artemis I, the uncrewed test mission, successfully orbited the Moon in December 2022.
            \"\"\"
        """.trimIndent()
        )
        runBlocking { exec.sendMessage(AgentChatSession(), problem).events.collect(AgentFlowLogger(verbose = true)) }
    }

}
