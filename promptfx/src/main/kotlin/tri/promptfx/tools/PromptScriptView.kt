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
import tri.ai.pips.AiTaskResult
import tri.ai.pips.aitask
import tri.ai.prompt.AiPrompt
import tri.ai.prompt.AiPromptLibrary
import tri.ai.prompt.run.AiPromptBatchCyclic
import tri.ai.prompt.run.RunnableExecutionPolicy
import tri.ai.prompt.run.execute
import tri.ai.prompt.trace.AiPromptModelInfo.Companion.MAX_TOKENS
import tri.ai.prompt.trace.AiPromptModelInfo.Companion.STOP
import tri.ai.prompt.trace.AiPromptModelInfo.Companion.TEMPERATURE
import tri.ai.prompt.trace.AiPromptTrace
import tri.promptfx.AiPlanTaskView
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

    private val template = SimpleStringProperty("")
    private val filter = SimpleStringProperty("")
    private val inputText = SimpleStringProperty("")

    private val chunkBy = SimpleStringProperty("\\n")
    private val chunkLimit = SimpleIntegerProperty(10)
    private val showUniqueResults = SimpleBooleanProperty(true)
    private val showAllResults = SimpleBooleanProperty(true)
    private val csvHeader = SimpleBooleanProperty(false)
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
        parameters("Model Parameters") {
            with(common) {
                temperature()
                maxTokens()
            }
        }
        parameters("Scripting Options") {
            field("Chunk Input by") {
                tooltip("Character(s) separating chunks of input, e.g. \\n for new lines or \\n\\n for paragraphs")
                textfield(chunkBy)
            }
            field("Input Format") {
                checkbox("Input has CSV Header", csvHeader)
            }
            field("Chunk Limit") {
                slider(1..1000, chunkLimit)
                label(chunkLimit.asString())
            }
            field("Display") {
                checkbox("Unique Results", showUniqueResults)
            }
            field("", forceLabelIndent = true) {
                checkbox("All Results", showAllResults)
            }
            field("", forceLabelIndent = true) {
                checkbox("As CSV", outputCsv)
            }
        }
    }

    override fun plan() = aitask("text-completion") {
        val result = RunnableExecutionPolicy().execute(promptBatch())
        AiTaskResult.result(result)
    }.task("process-results") {
        postProcess(it)
    }.planner

    private fun promptBatch() = AiPromptBatchCyclic().apply {
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

    private fun postProcess(results: List<AiPromptTrace>): String {
        val sectionCount = (if (showUniqueResults.value) 1 else 0) +
                (if (showAllResults.value) 1 else 0) +
                (if (outputCsv.value) 1 else 0)

        val displayString = StringBuilder()
        if (showUniqueResults.value) {
            val countEach = results.mapNotNull { it.outputInfo.output }
                .groupingBy { it.cleanedup() }
                .eachCount()
            if (sectionCount > 1) {
                displayString.append("Unique Results: ${countEach.size}\n")
                displayString.append("------------------\n")
            }
            countEach.entries.forEach { displayString.append("${it.key}: ${it.value}\n") }
        }
        if (showAllResults.value) {
            if (displayString.isNotEmpty())
                displayString.append("\n")
            if (sectionCount > 1) {
                displayString.append("All Results:\n")
                displayString.append("------------------\n")
            }
            results.forEach {
                displayString.append(it.outputInfo.output ?: "(error or no output returned)")
                displayString.append("\n")
            }
        }
        if (outputCsv.value) {
            if (displayString.isNotEmpty())
                displayString.append("\n")
            if (sectionCount > 1) {
                displayString.append("CSV Output:\n")
                displayString.append("------------------\n")
            }
            if (csvHeader.value)
                displayString.append(inputs().first ?: "input")
                    .append(",output\n")
            results.forEach {
                displayString.append("${it.promptInfo.promptParams["input"]},${it.outputInfo.output}\n")
            }
        }
        return displayString.toString()
    }

    private fun String.cleanedup() = lowercase().removeSuffix(".")

}
