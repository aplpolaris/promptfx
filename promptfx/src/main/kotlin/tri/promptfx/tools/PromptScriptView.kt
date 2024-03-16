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
import tri.ai.prompt.trace.batch.AiPromptBatchCyclic
import tri.ai.prompt.trace.AiPromptTrace
import tri.promptfx.AiPlanTaskView
import tri.promptfx.docs.DocumentInsightView.Companion.DOCUMENT_REDUCE_PREFIX
import tri.promptfx.docs.DocumentQaView
import tri.promptfx.ui.PromptTraceCardList
import tri.promptfx.ui.promptfield
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.enableDroppingFileContent
import tri.util.ui.slider
import java.lang.StringBuilder
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

    // pre-processing
    private val filter = SimpleStringProperty("")

    // batch processing
    private val chunkLimit = SimpleIntegerProperty(10)

    // completion template for individual items
    private val template = SimpleStringProperty("")

    // options for prompted summary of all results
    private val summaryPromptId = SimpleStringProperty("$DOCUMENT_REDUCE_PREFIX-summarize")
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

    init {
        input {
            spacing = 5.0
            paddingAll = 5.0
            vgrow = Priority.ALWAYS
            hbox {
                alignment = Pos.CENTER_LEFT
                spacing = 5.0
                text("Filter:")
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
        input {
            spacing = 5.0
            paddingAll = 5.0
            vgrow = Priority.ALWAYS
            hbox {
                alignment = Pos.CENTER_LEFT
                spacing = 5.0
                text("Template:")
                spacer()
                menubutton("", FontAwesomeIconView(FontAwesomeIcon.LIST)) {
                    // replace items when the menu is shown
                    setOnShowing {
                        items.clear()
                        AiPromptLibrary.INSTANCE.prompts.filter {
                            it.value.fields() == listOf("input")
                        }.keys.forEach { key ->
                            item(key) {
                                action { template.set(AiPromptLibrary.lookupPrompt(key).template) }
                            }
                        }
                    }
                }
            }
            textarea(template) {
                promptText = "Provide a prompt template, using {{input}} as a placeholder for user content."
                hgrow = Priority.ALWAYS
                prefRowCount = 10
                isWrapText = true
                prefWidth = 0.0
            }
        }
        input {
            spacing = 5.0
            paddingAll = 5.0
            vgrow = Priority.ALWAYS
            hbox {
                alignment = Pos.CENTER_LEFT
                spacing = 5.0
                text("Inputs:")
                spacer()
                button("", FontAwesomeIconView(FontAwesomeIcon.UPLOAD)) {
                    action {
                        val file = chooseFile("Select a file to load", filters = arrayOf())
                        if (file.isNotEmpty())
                            inputText.set(file.first().readText())
                    }
                }
            }
            // text box to preview input
            textarea(inputText) {
                promptText = "Enter a list of inputs to fill in the prompt (separated by line)."
                hgrow = Priority.ALWAYS
                vgrow = Priority.ALWAYS
                isWrapText = true
                prefWidth = 0.0
                enableDroppingFileContent()
            }
        }
    }

    init {
        outputPane.clear()
        output {
            add(PromptTraceCardList(promptTraces))
        }
        addOutputTextArea()
        onCompleted {
            val result = it.results.values.map { it.value }.filterIsInstance<AiPromptTrace>()
            promptTraces.setAll(result)
        }
    }

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
            tooltip("Loads from prompts.yaml with prefix $TEXT_JOINER_PREFIX and $DOCUMENT_REDUCE_PREFIX")
            promptfield("Text Joiner", joinerId, AiPromptLibrary.withPrefix(TEXT_JOINER_PREFIX), joinerText, workspace)
            promptfield("Summarizer", summaryPromptId, AiPromptLibrary.withPrefix(DOCUMENT_REDUCE_PREFIX), summaryPromptText, workspace)
        }
    }

    override fun plan(): AiPlanner {
        runLater { promptTraces.setAll() }
        return promptBatch().tasks()
            .map {
                it.monitor { runLater { promptTraces.add(it) } }
            }
            .aggregate()
            .task("process-results") {
                postProcess(it)
            }.planner
    }

    private fun promptBatch() = AiPromptBatchCyclic("prompt-script").apply {
        val inputs = inputs().second
        model = completionEngine.modelId
        modelParams = common.toModelParams()
        prompt = template.value
        promptParams = mapOf("input" to inputs)
        runs = inputs.size
    }

    /** Get the first chunk (if has header) and the rest of the chunks. */
    private fun inputs(): Pair<String?, List<String>> {
        var splitChar = chunkBy.value
        splitChar = if (splitChar.isEmpty())
            "\n"
        else
            splitChar.replace("\\n", "\n").replace("\\t", "\t")
        val split = inputText.value.split(splitChar)
        val header = if (csvHeader.value) split.first() else null
        return header to split.asSequence()
            .filter(filter())
            .drop(if (csvHeader.value) 1 else 0)
            .take(chunkLimit.value)
            .toList()
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
                    return { regex.matches(it) }
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
            AiPrompt(prompt).fill("input" to input)
                .let { completionEngine.complete(it, tokens = common.maxTokens.value, temperature = common.temp.value) }
                .value
        }
        return result?.contains("yes", ignoreCase = true) ?: false
    }

    private suspend fun postProcess(results: List<AiPromptTrace>): String {
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
            val csvHeader = if (csvHeader.value) inputs().first ?: "input" else ""
            val csv = results.joinToString("\n") { "${it.promptInfo.promptParams["input"]},${it.outputInfo.output}" }
            resultSets[key] = "$csvHeader\n$csv".trim()
        }
        if (summarizeResults.value) {
            val key = "LLM Summarized Results"
            val joined = joinerText.value.fill("matches" to
                results.map { mapOf("text" to (it.outputInfo.output ?: "")) }
            )
            val summarizer = summaryPromptText.value.fill("input" to joined)
            val summarizerResult = completionEngine.complete(summarizer, common.maxTokens.value, common.temp.value)
            resultSets[key] = summarizerResult.value ?: "(error or no output returned)"
        }
        return if (resultSets.size <= 1) {
            resultSets.values.firstOrNull() ?: ""
        } else {
            resultSets.entries.joinToString("\n\n") {
                "${it.key}\n" + "-".repeat(it.key.length) + "\n${it.value}"
            }
        }
    }

    private fun String.cleanedup() = lowercase().removeSuffix(".")

    companion object {
        private const val TEXT_SUMMARIZER_PREFIX = "document-reduce"
        private const val TEXT_JOINER_PREFIX = "text-joiner"
    }
}
