/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx.tools

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.geometry.Orientation
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.pips.*
import tri.ai.prompt.AiPrompt
import tri.ai.prompt.AiPromptLibrary
import tri.ai.prompt.trace.*
import tri.ai.prompt.trace.batch.AiPromptBatchCyclic
import tri.promptfx.AiPlanTaskView
import tri.promptfx.TextLibraryReceiver
import tri.promptfx.library.TextLibraryInfo
import tri.promptfx.ui.docs.TextLibraryToolbar
import tri.promptfx.ui.docs.TextLibraryViewModel
import tri.promptfx.ui.*
import tri.promptfx.ui.chunk.TextChunkListView
import tri.promptfx.ui.chunk.TextChunkViewModel
import tri.promptfx.ui.trace.PromptTraceCardList
import tri.util.ui.*

/** Plugin for the [PromptScriptView]. */
class PromptScriptPlugin : NavigableWorkspaceViewImpl<PromptScriptView>("Tools", "Prompt Scripting", WorkspaceViewAffordance.COLLECTION_ONLY, PromptScriptView::class)

/** A view designed to help you test prompt templates. */
class PromptScriptView : AiPlanTaskView("Prompt Scripting",
    "Configure a prompt to run on selected input (text collection, CSV file, etc.), and a second prompt to aggregate the results.",
), TextLibraryReceiver {

    private val viewScope = Scope(workspace)

    // inputs
    private val libraryModel: TextLibraryViewModel = find(viewScope)
    private val listModel = libraryModel.chunkListModel

    // batch processing
    private val chunkLimit = SimpleIntegerProperty(10)

    // completion template for individual items
    private lateinit var promptUi: EditablePromptUi

    // options for prompted summary of all results
    private val summaryPrompt = PromptSelectionModel("$TEXT_SUMMARIZER_PREFIX-summarize")
    private val joinerPrompt = PromptSelectionModel("$TEXT_JOINER_PREFIX-basic")

    // result list
    private val promptTraces = observableListOf<AiPromptTraceSupport<*>>()

    // result options
    private val showUniqueResults = SimpleBooleanProperty(true)
    private val showAllResults = SimpleBooleanProperty(true)
    private val summarizeResults = SimpleBooleanProperty(false)
    private val outputCsv = SimpleBooleanProperty(false)

    // input views
    init {
        input {
            add(find<TextLibraryToolbar>(viewScope).apply {
                titleText.set("Input")
            })
            promptUi = editablepromptui(
                promptFilter = { it.value.fields() == listOf(AiPrompt.INPUT) },
                instruction = "Prompt to Execute:"
            )
            add(find<TextChunkListView>(viewScope))
        }
    }

    // parameter views
    init {
        parameters("Batch Processing") {
            tooltip("These settings control the batch processing of inputs.")
            field("Limit") {
                tooltip("Maximum number of chunks to process")
                sliderwitheditablelabel(1..1000, chunkLimit)
            }
        }
        addDefaultTextCompletionParameters(common)
        parameters("Aggregation Types") {
            tooltip("These settings control how the results are displayed.")
            field("Display") {
                checkbox("Unique Results", showUniqueResults)
            }
            field("", forceLabelIndent = true) {
                checkbox("All Results", showAllResults)
            }
            field("", forceLabelIndent = true) {
                checkbox("LLM Summary", summarizeResults)
            }
            field("", forceLabelIndent = true) {
                checkbox("As CSV", outputCsv)
            }
        }
        parameters("LLM Aggregation Options") {
            enableWhen(summarizeResults)
            tooltip("Loads from prompts.yaml with prefix $TEXT_JOINER_PREFIX and $TEXT_SUMMARIZER_PREFIX")
            promptfield("Text Joiner", joinerPrompt, AiPromptLibrary.withPrefix(TEXT_JOINER_PREFIX), workspace)
            promptfield("Summarizer", summaryPrompt, AiPromptLibrary.withPrefix(TEXT_SUMMARIZER_PREFIX), workspace)
        }
    }

    // output views
    init {
        outputPane.clear()
        output {
            splitpane(Orientation.VERTICAL) {
                add(find<PromptTraceCardList>("prompts" to promptTraces))
                vbox {
                    toolbar {
                        text("Aggregated Result")
                    }
                    add(resultArea.root)
                    resultArea.root.vgrow = Priority.ALWAYS
                    vgrow = Priority.ALWAYS
                }
                vgrow = Priority.ALWAYS
            }
        }
        onCompleted {
            resultArea.setFinalResult(it.finalResult)
        }
    }

    override fun plan(): AiPlanner {
        val inputs = listModel.filteredChunkList.take(chunkLimit.value)
        if (inputs.isEmpty())
            return task("") { "(no input provided)" }.planner
        val docInputs = libraryModel.docSelection.map { doc ->
            val docChunks = doc.chunks.map { it.text(doc.all) }.toSet()
            ChunksWithHeader(doc.metadata.title, doc.dataHeader, inputs.filter { it.text in docChunks })
        }
        runLater { promptTraces.setAll() }
        val tasks = promptBatch(inputs.map { it.text }).tasks()
        // TODO - not sure what to do here with the view, since the traces probably shouldn't all be aggregated into one batch...
        // TODO - need to include the prompt trace as part of the output
        return tasks.map {
            it.monitorTrace { runLater { promptTraces.add(it) } }
        }.aggregatetrace()
        .aitaskonlist("process-results") {
            postProcess(it, docInputs)
        }.planner
    }

    override fun loadTextLibrary(library: TextLibraryInfo) {
        libraryModel.loadTextLibrary(library)
    }

    private fun promptBatch(inputs: List<String>) = AiPromptBatchCyclic("prompt-script").apply {
        model = completionEngine.modelId
        modelParams = common.toModelParams()
        prompt = promptUi.templateText.value
        promptParams = mapOf(AiPrompt.INPUT to inputs)
        runs = inputs.size
    }

    private suspend fun postProcess(results: List<AiPromptTraceSupport<String>>, inputs: List<ChunksWithHeader>): AiPromptTrace<String> {
        // TODO - for now assuming each trace has a single result

        val resultSets = mutableMapOf<String, String>()
        val values = results.map { it.values?.first() }

        if (showUniqueResults.value) {
            val countEach = values.filterNotNull()
                .groupingBy { it.cleanedup() }
                .eachCount()
            val key = "Unique Results: ${countEach.size}"
            resultSets[key] = countEach.entries.joinToString("\n") { "${it.key}: ${it.value}" }
        }
        if (showAllResults.value) {
            val key = "All Results"
            resultSets[key] = values.joinToString("\n") { it ?: "(error or no output returned)" }
        }
        if (outputCsv.value) {
            // TODO - how to handle multiple docs with varying headers??
            val key = "CSV Output"
            resultSets[key] = generateCsvOutput(inputs, results)
        }
        val promptInfo: AiPromptInfo
        if (summarizeResults.value) {
            val key = "LLM Summarized Results"
            val joined = joinerPrompt.fill("matches" to
                results.map { mapOf("text" to it.firstValue) }
            )
            val summarizer = summaryPrompt.fill(AiPrompt.INPUT to joined)
            val summarizerResult = completionEngine.complete(summarizer, common.maxTokens.value, common.temp.value)
            resultSets[key] = summarizerResult.firstValue
            promptInfo = AiPromptInfo(summaryPrompt.text.value, mapOf(AiPrompt.INPUT to joined))
        } else {
            promptInfo = AiPromptInfo("")
        }
        val output = if (resultSets.size <= 1) {
            resultSets.values.firstOrNull() ?: ""
        } else {
            resultSets.entries.joinToString("\n\n") {
                "${it.key}\n" + "-".repeat(it.key.length) + "\n${it.value}"
            }
        }
        return AiPromptTrace(
            promptInfo,
            AiModelInfo(completionEngine.modelId, common.toModelParams()),
            AiExecInfo(),
            AiOutputInfo.output(output)
        )
    }

    /** Generate CSV output (first input object only. */
    private fun generateCsvOutput(inputs: List<ChunksWithHeader>, results: List<AiPromptTraceSupport<String>>): String {
        val csvHeader = inputs.first().headerRow?.let { "$it,output" } ?: "input,output"
        val csv = results.joinToString("\n") { "${it.prompt!!.promptParams[AiPrompt.INPUT]},${it.firstValue}" }
        return "$csvHeader\n$csv".trim()
    }

    companion object {
        private const val TEXT_SUMMARIZER_PREFIX = "document-reduce"
        private const val TEXT_JOINER_PREFIX = "text-joiner"

        private fun String.cleanedup() = lowercase().removeSuffix(".")
    }
}

private class ChunksWithHeader(
    val docName: String?,
    val headerRow: String?,
    val chunks: List<TextChunkViewModel>
)
