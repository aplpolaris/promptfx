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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.pips.AiPlanner
import tri.ai.pips.aggregate
import tri.ai.prompt.AiPrompt
import tri.ai.prompt.AiPrompt.Companion.fill
import tri.ai.prompt.AiPromptLibrary
import tri.ai.prompt.trace.*
import tri.ai.prompt.trace.batch.AiPromptBatchCyclic
import tri.ai.text.chunks.TextChunkRaw
import tri.ai.text.chunks.TextLibrary
import tri.promptfx.AiPlanTaskView
import tri.promptfx.PromptFxConfig
import tri.promptfx.PromptFxConfig.Companion.FF_ALL
import tri.promptfx.promptFxFileChooser
import tri.promptfx.ui.*
import tri.util.ui.*
import java.util.regex.PatternSyntaxException

/** Plugin for the [PromptScriptView]. */
class PromptScriptPlugin : NavigableWorkspaceViewImpl<PromptScriptView>("Tools", "Prompt Scripting", PromptScriptView::class)

/** A view designed to help you test prompt templates. */
class PromptScriptView : AiPlanTaskView("Prompt Scripting",
    "Configure a prompt to run on a series of inputs or a CSV file.") {

    // inputs
    private val chunkBy = SimpleStringProperty("\\n")
    private val csvHeader = SimpleBooleanProperty(false)
    private val inputText = SimpleStringProperty("")
    private val inputLibrary = SimpleObjectProperty<TextLibrary>()

    // inputs as chunks
    private val inputChunks = observableListOf<TextChunkViewModel>()

    // pre-processing
    private val filter = SimpleStringProperty("")
    private val filterTypeText = filter.stringBinding {
        when {
            it.isNullOrBlank() -> "None"
            "{{input}}" in it -> "LLM Filter"
            else -> "Regex Match"
        }
    }

    // batch processing
    private val chunkLimit = SimpleIntegerProperty(10)

    // completion template for individual items
    private lateinit var promptUi: EditablePromptUi

    // options for prompted summary of all results
    private val summaryPromptId = SimpleStringProperty("$TEXT_SUMMARIZER_PREFIX-summarize")
    private val summaryPromptText = summaryPromptId.stringBinding { AiPromptLibrary.lookupPrompt(it!!).template }

    private val joinerId = SimpleStringProperty("$TEXT_JOINER_PREFIX-basic")
    private val joinerText = joinerId.stringBinding { AiPromptLibrary.lookupPrompt(it!!).template }

    // result list
    private val promptTraces = observableListOf<AiPromptTrace>()

    // result options
    private val showUniqueResults = SimpleBooleanProperty(true)
    private val showAllResults = SimpleBooleanProperty(true)
    private val summarizeResults = SimpleBooleanProperty(false)
    private val outputCsv = SimpleBooleanProperty(false)

    // input views
    init {
        input {
            squeezebox {
                fold("Prompt Settings", expanded = true) {
                    promptUi = EditablePromptUi(
                        promptFilter = { it.value.fields() == listOf(AiPrompt.INPUT) },
                        instruction = "Prompt to Execute:"
                    )
                    add(promptUi)
                }
                fold("Select Inputs", expanded = true) {
                    vbox(5) {
                        vgrow = Priority.ALWAYS
                        hbox(5, Pos.CENTER_LEFT) {
                            text("Inputs:")
                            spacer()
                            button("", FontAwesomeIconView(FontAwesomeIcon.UPLOAD)) {
                                tooltip("Load text library file. This will hide any current input text.")
                                action {
                                    promptFxFileChooser(
                                        dirKey = PromptFxConfig.DIR_KEY_TEXTLIB,
                                        title = "Load Text Library",
                                        filters = arrayOf(PromptFxConfig.FF_JSON, FF_ALL),
                                        mode = FileChooserMode.Single
                                    ) {
                                        it.firstOrNull()?.let {
                                            val lib = TextLibrary.loadFrom(it)
                                            inputLibrary.set(lib)
                                        }
                                    }
                                }
                            }
                            button("", FontAwesomeIconView(FontAwesomeIcon.FILE_TEXT)) {
                                tooltip("Load text from file. This will clear any text library that has been previously loaded.")
                                action {
                                    inputLibrary.set(null)
                                    promptFxFileChooser(
                                        title = "Select a file to load",
                                        filters = arrayOf(FF_ALL)
                                    ) {
                                        it.firstOrNull()?.readText()?.let {
                                            inputText.set(it)
                                        }
                                    }
                                }
                            }
                        }
                        label(inputLibrary.stringBinding {
                            if (it == null)
                                "No library loaded"
                            else
                                "Library: ${it.metadata.id.ifBlank { it.metadata.path }}, ${it.docs.size} documents, ${it.docs.sumOf { it.chunks.size }} chunks"
                        }) {
                            visibleWhen { inputLibrary.isNotNull }
                            managedWhen { inputLibrary.isNotNull }
                        }
                        textarea(inputText) {
                            visibleWhen { inputLibrary.isNull }
                            managedWhen { inputLibrary.isNull }
                            promptText = "Enter a list of inputs to fill in the prompt (separated by line)."
                            hgrow = Priority.ALWAYS
                            vgrow = Priority.ALWAYS
                            isWrapText = true
                            prefWidth = 0.0
                            enableDroppingFileContent()
                        }
                    }
                    vbox(5) {
                        hbox(5, Pos.CENTER_LEFT) {
                            text("Filter:")
                            spacer()
                            text(filterTypeText) {
                                style {
                                    fontStyle = javafx.scene.text.FontPosture.ITALIC
                                }
                            }
                            button("", FontAwesomeIcon.FILTER.graphic) {
                                disableWhen { filterTypeText.isEqualTo("None") }
                                tooltip("Apply filter")
                                action { runLater { inputChunks.setAll(inputs().chunks) } }
                            }
                        }
                        textarea(filter) {
                            promptText =
                                "(Optional) Enter a regular expression to filter content (faster), or provide a prompt with {{input}} returning yes/no (slower). If blank, empty lines will be skipped."
                            hgrow = Priority.ALWAYS
                            prefRowCount = 5
                            isWrapText = true
                            prefWidth = 0.0
                        }
                    }
                }
                fold("Input Preview", expanded = false) {
                    add(TextChunkListView(inputChunks, hostServices))
                }
            }
        }
    }

    // parameter views
    init {
        parameters("Data Import") {
            tooltip("These settings control how input is divided into separate chunks of text for further processing.")
            field("Chunk Input by") {
                tooltip("Character(s) separating chunks of input, e.g. \\n for new lines or \\n\\n for paragraphs")
                textfield(chunkBy)
            }
            field("Input Format") {
                checkbox("Input has Header Row", csvHeader)
            }
        }
        parameters("Filtering") { }
        parameters("Batch Processing") {
            tooltip("These settings control the batch processing of inputs.")
            field("Limit") {
                tooltip("Maximum number of chunks to process")
                slider(1..1000, chunkLimit)
                label(chunkLimit.asString())
            }
        }
        addDefaultTextCompletionParameters(common)
        parameters("Output Options") {
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
        parameters("Result Summarization Template") {
            enableWhen(summarizeResults)
            tooltip("Loads from prompts.yaml with prefix $TEXT_JOINER_PREFIX and $TEXT_SUMMARIZER_PREFIX")
            promptfield("Text Joiner", joinerId, AiPromptLibrary.withPrefix(TEXT_JOINER_PREFIX), joinerText, workspace)
            promptfield("Summarizer", summaryPromptId, AiPromptLibrary.withPrefix(TEXT_SUMMARIZER_PREFIX), summaryPromptText, workspace)
        }
    }

    // output views
    init {
        outputPane.clear()
        output {
            add(PromptTraceCardList(promptTraces))
        }
        addOutputTextArea()
        outputPane.children.last().vgrow = Priority.ALWAYS
    }

    override fun plan(): AiPlanner {
        val inputs = inputs()
        runLater {
            promptTraces.setAll()
            inputChunks.setAll(inputs.chunks)
        }
        val tasks = promptBatch(inputs.chunks.map { it.text }).tasks()
        return tasks
            .map {
                it.monitor { runLater { promptTraces.add(it) } }
            }
            .aggregate()
            .task("process-results") {
                postProcess(it, inputs)
            }.planner
    }

    private fun promptBatch(inputs: List<String>) = AiPromptBatchCyclic("prompt-script").apply {
        model = completionEngine.modelId
        modelParams = common.toModelParams()
        prompt = promptUi.templateText.value
        promptParams = mapOf(AiPrompt.INPUT to inputs)
        runs = inputs.size
    }

    /** Get the first chunk (if has header) and the rest of the chunks. */
    private fun inputs(): PromptScriptInput {
        val filter = filter()
        if (inputLibrary.value != null) {
            val chunks = inputLibrary.value!!.docs.flatMap {
                doc -> doc.chunks.map { it.asTextChunkViewModel(doc, embeddingService.modelId) }
            }.asSequence()
                .filter { filter(it.text) }
                .take(chunkLimit.value)
            return PromptScriptInput(null, chunks.toList())
        } else {
            var splitChar = chunkBy.value
            splitChar = if (splitChar.isEmpty())
                "\n"
            else
                splitChar.replace("\\n", "\n").replace("\\t", "\t")
            val split = inputText.value.split(splitChar).map { TextChunkRaw(it) }
            val header = if (csvHeader.value) split.first().text else null
            val chunks = split.asSequence()
                .filter { filter(it.text) }
                .drop(if (csvHeader.value) 1 else 0)
                .take(chunkLimit.value)
                .map { it.asTextChunkViewModel(null, embeddingService.modelId) }
                .toList()
            return PromptScriptInput(header, chunks)
        }
    }

    private fun filter(): (String) -> Boolean {
        val filter = filter.value
        when {
            filter.isBlank() ->
                return { it.isNotBlank() }
            "{{input}}" in filter ->
                return { llmFilter(filter, it) }
            else -> {
                try {
                    val regex = filter.toRegex()
                    return { regex.find(it) != null }
                } catch (x: PatternSyntaxException) {
                    tri.util.warning<PromptScriptView>("Invalid regex: ${x.message}")
                    return { false }
                }
            }
        }
    }

    /**
     * Attempt to filter an input based on a given prompt.
     * Returns true if the response contains "yes" (case-insensitive) anywhere.
     */
    private fun llmFilter(prompt: String, input: String): Boolean {
        val result = runBlocking {
            AiPrompt(prompt).fill(AiPrompt.INPUT to input)
                .let { completionEngine.complete(it, tokens = common.maxTokens.value, temperature = common.temp.value) }
                .value
        }
        return result?.contains("yes", ignoreCase = true) ?: false
    }

    private suspend fun postProcess(results: List<AiPromptTrace>, inputs: PromptScriptInput): AiPromptTrace {
        val resultSets = mutableMapOf<String, String>()

        if (showUniqueResults.value) {
            val countEach = results.mapNotNull { it.outputInfo.output }
                .groupingBy { it.cleanedup() }
                .eachCount()
            val key = "Unique Results: ${countEach.size}"
            resultSets[key] = countEach.entries.joinToString("\n") { "${it.key}: ${it.value}" }
        }
        if (showAllResults.value) {
            val key = "All Results"
            resultSets[key] = results.joinToString("\n") { it.outputInfo.output ?: "(error or no output returned)" }
        }
        if (outputCsv.value) {
            val key = "CSV Output"
            val csvHeader = inputs.headerRow?.let { "$it,output" } ?: "input,output"
            val csv = results.joinToString("\n") { "${it.promptInfo.promptParams[AiPrompt.INPUT]},${it.outputInfo.output}" }
            resultSets[key] = "$csvHeader\n$csv".trim()
        }
        val promptInfo: AiPromptInfo
        if (summarizeResults.value) {
            val key = "LLM Summarized Results"
            val joined = joinerText.value.fill("matches" to
                results.map { mapOf("text" to (it.outputInfo.output ?: "")) }
            )
            val summarizer = summaryPromptText.value.fill(AiPrompt.INPUT to joined)
            val summarizerResult = completionEngine.complete(summarizer, common.maxTokens.value, common.temp.value)
            resultSets[key] = summarizerResult.value ?: "(error or no output returned)"
            promptInfo = AiPromptInfo(summaryPromptText.value, mapOf(AiPrompt.INPUT to joined))
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
            AiPromptModelInfo(completionEngine.modelId, common.toModelParams()),
            AiPromptExecInfo(),
            AiPromptOutputInfo(output)
        )
    }

    private fun String.cleanedup() = lowercase().removeSuffix(".")

    companion object {
        private const val TEXT_SUMMARIZER_PREFIX = "document-reduce"
        private const val TEXT_JOINER_PREFIX = "text-joiner"
    }
}

/** Inputs for executing prompt script. */
private class PromptScriptInput(
    val headerRow: String?,
    val chunks: List<TextChunkViewModel>
)
