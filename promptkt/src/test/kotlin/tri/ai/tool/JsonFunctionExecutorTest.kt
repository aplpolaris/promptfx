/*-
 * #%L
 * tri.promptfx:promptkt
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

import com.aallam.openai.api.logging.LogLevel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tri.ai.openai.OpenAiClient
import tri.ai.openai.OpenAiModels.GPT35_TURBO
import tri.ai.openai.OpenAiTextPlugin
import tri.ai.prompt.AiPromptLibrary

@Disabled("Requires apikey")
class JsonFunctionExecutorTest {

    companion object {
        val GPT35 = OpenAiTextPlugin().textCompletionModels().first()

        val SAMPLE_TOOL1 = tool("calc", "Use this to do math",
            """{"type":"object","properties":{"input":{"type":"string"}}}""") {
            "42"
        }
        val SAMPLE_TOOL2 = tool("romanize", "Converts numbers to Roman numerals",
            """{"type":"object","properties":{"input":{"type":"integer"}}}""") {
            val value = it["input"]?.jsonPrimitive?.int ?: throw RuntimeException("No input")
            when (value) {
                5 -> "V"
                42 -> "XLII"
                84 -> "LXXXIV"
                else -> "I don't know"
            }
        }
        val SAMPLE_TOOL3 = tool("user", "Prompt the user for additional information you need",
            """{"type":"object","properties":{"user_request":{"type":"string"}}}""") {
            // get user input from console
            val input = it["user_request"]?.jsonPrimitive?.content ?: it.toString()
            print("I need some more information: $input >> ")
            readlnOrNull() ?: ""
        }
        val SAMPLE_TOOL4 = tool("other", "Answer a question that cannot be answered by the other tools",
            """{"type":"object","properties":{"input":{"type":"string"}}}""") {
            "I don't know"
        }
        val SAMPLE_TOOLS = listOf(SAMPLE_TOOL1, SAMPLE_TOOL2, SAMPLE_TOOL4)

        fun tool(name: String, description: String, schema: String, op: (JsonObject) -> String) = object : JsonTool(name, description, schema) {
            override suspend fun run(input: JsonObject) = op(input)
        }
    }

    @Test
    fun testTools() {
        OpenAiClient.INSTANCE.settings.logLevel = LogLevel.None

        val exec = JsonFunctionExecutor(OpenAiClient.INSTANCE, GPT35_TURBO, SAMPLE_TOOLS)

        runBlocking {
            exec.execute("Multiply 21 times 2 and then convert it to Roman numerals.")
            exec.execute("Convert 5 to a Roman numeral.")
            exec.execute("What year was Jurassic Park?")
        }
    }

    @Test
    fun testTools3() {
        OpenAiClient.INSTANCE.settings.logLevel = LogLevel.None

        val tool1 = tool("Abstract", "Use this to extract the abstract of a research paper (input is a reference to the paper)",
            """{"type":"object","properties":{"paper_ref":{"type":"string"}}}""") {
            """
                Common layout techniques for dynamic networks typically either keep node positions static as the graph changes, or operate by “tweening” optimized layouts between adjacent time slices. These techniques can be problematic because (in the first case) there is significant visual “noise” caused by unnecessary edge crossings, and (in the second case) the nodes change so much from one time slice to another that animation is required to display node movement. This paper describes techniques to balance the benefits of keeping node positions relatively static while allowing enough layout adjustment between slices to demonstrate the changing graph. Comparisons are provided against the common layout procedures for a graph with 20 time slices.
            """.trimIndent()
        }
        val tool1b = tool("Sectionizer", "Get text from a specific section of a document (input is a reference to the paper and a single section)",
            """{"type":"object","properties":{"paper_ref":{"type":"string"},"section":{"type":"string"}}}""") {
            val section = it["section"]?.jsonPrimitive?.content ?: throw RuntimeException("No section")
            when {
                "introduction" in section.lowercase() -> """
                    Understanding the role of change over time has always been a key factor in the analysis of social networks, yet techniques for the visualization of dynamic networks have lagged significantly behind those for static networks. At the same time, the need for better insight into dynamic networks is also growing, with the advent of new statistical techniques for understanding dynamic graphs. An increasing number of sociologists are turning to Markov models such as the exponential random graph model to understand the changing nature of social fabric [12][13]. This combination of old and new research techniques begs for new visualization techniques designed specifically for dynamic graphs.
                    It is not surprising that dynamic graph visualization remains underdeveloped, since the easiest graphical means to represent a social network on paper is a single snapshot of the graph, and the leap from static image to dynamic or animating images is substantial. Static graph visualization, on the other hand, has a long history. Freeman [5] traces the development of static graph visualization from early ad hoc techniques to computational layout techniques and finally the more modern “force-directed” techniques. He also cites numerous tools that have been developed to visualize networks, a number that has increased considerably in the past decade [1][2].
                    While the information in a social network can be easily displayed as a matrix or some other kind of data structure, these kinds of data displays are much more limited in the kind of insight that they can support [2][5][14]. Research in neuroscience also supports the need for visualization; the human visual processing circuitry is specially-adapted to apprehending and understanding certain kinds of visual patterns [15]. Visualization techniques for dynamic networks that leverage the same processing capabilities of the human brain are likely to have the same impact on insight and understanding for dynamic networks.
                    There are three commonly-cited layout techniques for dynamic network visualization [2]. The first is independent layout, in which each slice of a dynamic graph is visualized independently. Second, in a chaining layout, each slice is visualized independently, but with the output positions of one slice fed in as the input positions of the subsequent slice [8]. Third, in a tweening layout, a suitably stable layout algorithm is used for each slice independently, and a tweening algorithm is used to animate the transition from one slice to the next [2].
                    One of the main drawbacks to these techniques is that individual nodes may move rapidly from one visual slice to the next, even when the node itself does not change very much. This paper introduces a new algorithm, which we call the time spring algorithm, which seeks to achieve a balance between individual node movement and maintaining the structure of the current graph. The central idea is to layout each slice of a dynamic graph simultaneously, attaching additional “springs” between a single node in adjacent time slices to keep it from moving too much. An added benefit of this technique is that it offers good results both for animating visualizations as well as small multiple visualizations.
                    This paper is organized as follows. Section 2 reviews dynamic graphs, including several mechanisms by which they can be defined/sampled. Sections 3 and 4 provide a brief overview of existing static and dynamic graph visualization techniques. Section 5 describes the time spring algorithm, and evaluates it using a sample dynamic graph.
                """.trimIndent()
                "conclusion" in section.lowercase() -> """
                    The time spring layout algorithm is subject to many of the same limitations as standard graph layout algorithms. If the number of edges is much greater than the number of vertices, the resulting visualization has many edge crossing and obscures the underlying topology. This is to be expected for any planar visualization, however. Qualitatively, the time spring algorithm works very well at minimizing node movement, while accommodating enough change to also minimize poor edge placement. One can also see some of the large-scale trends in the dataset with the small multiples view of the dataset. It is likely that experimenting with the algorithm’s parameters might produce even better results. By balancing the dual visual priorities of minimizing node movement and edge crossings, the algorithm provides a new aid to intuition for researchers studying dynamic networks.
                """.trimIndent()
                else -> "This is placeholder content for this section of the report."
            }
        }
        val tool2 = tool("MainPoint", "Use this to extract the main points from text (input is raw text)",
            """{"type":"object","properties":{"input":{"type":"string"}}}""") {
            val input = it["input"]?.jsonPrimitive?.content ?: throw RuntimeException("No input")
            AiPromptLibrary.lookupPrompt("summarization").fill(
                "input" to input,
                "instruct" to "Extract the main point and research implications of the text in 1-2 concise sentences."
            ).let { runBlocking { GPT35.complete(it).value!! } }
        }
        val tool3 = tool("Concepts", "Use this to extract the main concepts from text",
            """{"type":"object","properties":{"text":{"type":"string"}}}""") {
            val input = it["text"]?.jsonPrimitive?.content ?: throw RuntimeException("No input")
            AiPromptLibrary.lookupPrompt("text-to-json").fill(
                "format" to "a list of concepts",
                "input" to input
            ).let { runBlocking { GPT35.complete(it).value!! } }
        }
        val tool4 = tool("Sentiment", "Use this to extract the sentiment from text",
            """{"type":"object","properties":{"text":{"type":"string"}}}""") {
            val input = it["text"]?.jsonPrimitive?.content ?: throw RuntimeException("No input")
            AiPromptLibrary.lookupPrompt("sentiment-classify").fill(
                "input" to input,
                "instruct" to "positive, negative, or neutral"
            ).let { runBlocking { GPT35.complete(it).value!! } }
        }
        val tool5 = tool("CitationFinder", "Use this to extract the citations from text",
            """{"type":"object","properties":{"text":{"type":"string"}}}""") {
            val input = it["text"]?.jsonPrimitive?.content ?: throw RuntimeException("No input")
            AiPromptLibrary.lookupPrompt("text-to-json").fill(
                "format" to "a list of citations",
                "input" to input
            ).let { runBlocking { GPT35.complete(it).value!! } }
        }
        val tool6 = tool("ConceptMap", "Use this to generate a concept map from text",
            """{"type":"object","properties":{"text":{"type":"string"}}}""") {
            val input = it["text"]?.jsonPrimitive?.content ?: throw RuntimeException("No input")
            AiPromptLibrary.lookupPrompt("text-to-json").fill(
                "format" to "a PlantUML mindmap diagram",
                "input" to input
            ).let { runBlocking { GPT35.complete(it).value!! } }
        }
        val tool7 = tool("ReportGenerator", "Use this to write a formal report on the user's question, based on findings from previous responses",
            """{"type":"object","properties":{"text":{"type":"string"},"findings":{"type":"string"}}}""") {
            "I have generated a report and saved it to the file CoolReport.pdf."
        }
        val tool8 = tool("Other", "Answer a question that cannot be answered by the other tools",
            """{"type":"object","properties":{"input":{"type":"string"}}}""") {
            "I don't know"
        }

        val tool_list = listOf(tool1, tool1b, tool2, tool3, tool4, tool5, tool6, tool7, tool8)
        val exec = JsonFunctionExecutor(OpenAiClient.INSTANCE, GPT35_TURBO, tool_list)

        runBlocking {
            exec.execute("Locate the introduction and conclusion sections to identify CoolMath.pdf's goals and key findings. Summarize these sections to provide a concise overview.")
            exec.execute("Identify and extract the main concepts from the paper CoolMath.pdf, then use these concepts to generate a visual map that illustrates the structure of the study and the connections between its different parts.")
            exec.execute("Analyze the sentiment of the text in CoolMath.pdf to determine the tone used when discussing the research implications.")
        }
    }

}

//fun main(args: Array<String>) {
//    val exec = JsonFunctionExecutor(OpenAiClient.INSTANCE, GPT35_TURBO, listOf(
//        JsonFunctionExecutorTest.SAMPLE_TOOL1,
//        JsonFunctionExecutorTest.SAMPLE_TOOL2,
//        JsonFunctionExecutorTest.SAMPLE_TOOL3,
//        JsonFunctionExecutorTest.SAMPLE_TOOL4
//    ))
//    runBlocking {
//        exec.execute("Multiply the user's favorite number by 5.")
//    }
//}
