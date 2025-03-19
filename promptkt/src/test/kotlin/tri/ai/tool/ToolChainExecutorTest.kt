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

import com.aallam.openai.api.logging.LogLevel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.openai.OpenAiClient
import tri.ai.openai.OpenAiCompletionChat
import tri.ai.openai.OpenAiPlugin
import tri.ai.prompt.AiPromptLibrary

@Tag("openai")
class ToolChainExecutorTest {

    val GPT35 = OpenAiPlugin().textCompletionModels().first()

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
            override suspend fun run(input: String) = OpenAiCompletionChat().complete(input, tokens = 500, history = listOf()).firstValue!!
        }
        val tool2 = object : Tool("Timeline", "Use this once you have all the data needed to show the result on a timeline. Provide structured data as input.", isTerminal = true) {
            override suspend fun run(input: String) = OpenAiCompletionChat().complete("""
                Create a JSON object that can be used to plot a timeline of the following information:
                $input
                The result should confirm to the vega-lite spec, using either a Gantt chart or a dot plot.
                Each event, date, or date range should be shown as a separate entry on the y-axis, sorted by date.
                Provide the JSON result only, no explanation.
            """.trimIndent(), tokens = 1000, history = listOf()).firstValue!!
        }
        ToolChainExecutor(OpenAiCompletionChat())
            .executeChain("Look up data with the birth years of the first 10 US presidents along with the order of their presidency, and then visualize the results.", listOf(tool1, tool2))
    }

    @Test
    fun testTools3() {
        OpenAiClient.INSTANCE.settings.logLevel = LogLevel.None

        val tool1 = tool("Abstract", "Use this to extract the abstract of a research paper (input is a reference to the paper)") {
            """
                Common layout techniques for dynamic networks typically either keep node positions static as the graph changes, or operate by “tweening” optimized layouts between adjacent time slices. These techniques can be problematic because (in the first case) there is significant visual “noise” caused by unnecessary edge crossings, and (in the second case) the nodes change so much from one time slice to another that animation is required to display node movement. This paper describes techniques to balance the benefits of keeping node positions relatively static while allowing enough layout adjustment between slices to demonstrate the changing graph. Comparisons are provided against the common layout procedures for a graph with 20 time slices.
            """.trimIndent()
        }
        val tool1b = tool("Sectionizer", "Get text from a specific section of a document (input is a reference to the paper and a single section)") {
            when {
                "introduction" in it.lowercase() -> """
                    Understanding the role of change over time has always been a key factor in the analysis of social networks, yet techniques for the visualization of dynamic networks have lagged significantly behind those for static networks. At the same time, the need for better insight into dynamic networks is also growing, with the advent of new statistical techniques for understanding dynamic graphs. An increasing number of sociologists are turning to Markov models such as the exponential random graph model to understand the changing nature of social fabric [12][13]. This combination of old and new research techniques begs for new visualization techniques designed specifically for dynamic graphs.
                    It is not surprising that dynamic graph visualization remains underdeveloped, since the easiest graphical means to represent a social network on paper is a single snapshot of the graph, and the leap from static image to dynamic or animating images is substantial. Static graph visualization, on the other hand, has a long history. Freeman [5] traces the development of static graph visualization from early ad hoc techniques to computational layout techniques and finally the more modern “force-directed” techniques. He also cites numerous tools that have been developed to visualize networks, a number that has increased considerably in the past decade [1][2].
                    While the information in a social network can be easily displayed as a matrix or some other kind of data structure, these kinds of data displays are much more limited in the kind of insight that they can support [2][5][14]. Research in neuroscience also supports the need for visualization; the human visual processing circuitry is specially-adapted to apprehending and understanding certain kinds of visual patterns [15]. Visualization techniques for dynamic networks that leverage the same processing capabilities of the human brain are likely to have the same impact on insight and understanding for dynamic networks.
                    There are three commonly-cited layout techniques for dynamic network visualization [2]. The first is independent layout, in which each slice of a dynamic graph is visualized independently. Second, in a chaining layout, each slice is visualized independently, but with the output positions of one slice fed in as the input positions of the subsequent slice [8]. Third, in a tweening layout, a suitably stable layout algorithm is used for each slice independently, and a tweening algorithm is used to animate the transition from one slice to the next [2].
                    One of the main drawbacks to these techniques is that individual nodes may move rapidly from one visual slice to the next, even when the node itself does not change very much. This paper introduces a new algorithm, which we call the time spring algorithm, which seeks to achieve a balance between individual node movement and maintaining the structure of the current graph. The central idea is to layout each slice of a dynamic graph simultaneously, attaching additional “springs” between a single node in adjacent time slices to keep it from moving too much. An added benefit of this technique is that it offers good results both for animating visualizations as well as small multiple visualizations.
                    This paper is organized as follows. Section 2 reviews dynamic graphs, including several mechanisms by which they can be defined/sampled. Sections 3 and 4 provide a brief overview of existing static and dynamic graph visualization techniques. Section 5 describes the time spring algorithm, and evaluates it using a sample dynamic graph.
                """.trimIndent()
                "conclusion" in it.lowercase() -> """
                    The time spring layout algorithm is subject to many of the same limitations as standard graph layout algorithms. If the number of edges is much greater than the number of vertices, the resulting visualization has many edge crossing and obscures the underlying topology. This is to be expected for any planar visualization, however. Qualitatively, the time spring algorithm works very well at minimizing node movement, while accommodating enough change to also minimize poor edge placement. One can also see some of the large-scale trends in the dataset with the small multiples view of the dataset. It is likely that experimenting with the algorithm’s parameters might produce even better results. By balancing the dual visual priorities of minimizing node movement and edge crossings, the algorithm provides a new aid to intuition for researchers studying dynamic networks.
                """.trimIndent()
                else -> "This is placeholder content for this section of the report."
            }
        }
        val tool2 = tool("Main Point", "Use this to extract the main points from text (input is raw text)") {
            AiPromptLibrary.lookupPrompt("summarization").fill(
                "input" to it,
                "instruct" to "Extract the main point and research implications of the text in 1-2 concise sentences."
            ).let { runBlocking { GPT35.complete(it).firstValue!! } }
        }
        val tool3 = tool("Concepts", "Use this to extract the main concepts from text (input is raw text)") {
            AiPromptLibrary.lookupPrompt("text-to-json").fill(
                "format" to "a list of concepts",
                "input" to it
            ).let { runBlocking { GPT35.complete(it).firstValue!! } }
        }
        val tool4 = tool("Sentiment", "Use this to extract the sentiment from text (input is raw text)") {
            AiPromptLibrary.lookupPrompt("sentiment-classify").fill(
                "input" to it,
                "instruct" to "positive, negative, or neutral"
            ).let { runBlocking { GPT35.complete(it).firstValue!! } }
        }
        val tool5 = tool("Citation Finder", "Use this to extract the citations from text (input is raw text)") {
            AiPromptLibrary.lookupPrompt("text-to-json").fill(
                "format" to "a list of citations",
                "input" to it
            ).let { runBlocking { GPT35.complete(it).firstValue!! } }
        }
        val tool6 = tool("Concept Map", "Use this to generate a concept map from text (input is any text)") {
            AiPromptLibrary.lookupPrompt("text-to-json").fill(
                "format" to "a PlantUML mindmap diagram",
                "input" to it
            ).let { runBlocking { GPT35.complete(it).firstValue!! } }
        }
        val tool7 = tool("Report Generator", "Use this to write a formal report on the user's question (input is any text)") {
            "I have generated a report and saved it to the file CoolReport.pdf."
        }
        val tool8 = tool("Other", "Answer a question that cannot be answered by the other tools") {
            "I don't know"
        }

        val tool_list = listOf(tool1, tool1b, tool2, tool3, tool4, tool5, tool6, tool7, tool8)

        ToolChainExecutor(OpenAiCompletionChat())
            .executeChain("Locate the introduction and conclusion sections to identify CoolMath.pdf's goals and key findings. Summarize these sections to provide a concise overview.",
                tool_list)
        ToolChainExecutor(OpenAiCompletionChat())
            .executeChain("Identify and extract the main concepts from the paper CoolMath.pdf, then use these concepts to generate a visual map that illustrates the structure of the study and the connections between its different parts.",
                tool_list)
        ToolChainExecutor(OpenAiCompletionChat())
            .executeChain("Locate and extract text from the abstract and conclusion of the paper CoolMath.pdf. Analyze the sentiment of these sections to determine the tone used when discussing the research implications.",
                tool_list)
    }

    fun tool(name: String, description: String, op: (String) -> String) = object : Tool(name, description) {
        override suspend fun run(input: String) = op(input)
    }

}
