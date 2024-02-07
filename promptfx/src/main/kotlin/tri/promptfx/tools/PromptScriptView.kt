/*-
 * #%L
 * promptfx-0.1.8
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
import tri.promptfx.AiPlanTaskView
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.slider
import java.time.LocalDate
import java.util.regex.PatternSyntaxException

/** Plugin for the [PromptScriptView]. */
class PromptScriptPlugin : NavigableWorkspaceViewImpl<PromptScriptView>("Tools", "Prompt Scripting", PromptScriptView::class)

/** A view designed to help you test prompt templates. */
class PromptScriptView : AiPlanTaskView("Prompt Scripting",
    "Configure a prompt with a series of inputs.") {

    val template = SimpleStringProperty("")
    val filter = SimpleStringProperty("")
    val inputTextLines = SimpleStringProperty("")
    val lineLimit = SimpleIntegerProperty(10)

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
                promptText = "(Optional) Enter a regular expression to filter content (faster), or provide a prompt with {{input}} returning yes/no (slower). If blank, empty lines will be skipped."
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
                    AiPromptLibrary.INSTANCE.prompts.keys.forEach { key ->
                        item(key) {
                            action { template.set(AiPromptLibrary.lookupPrompt(key).template) }
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
                            inputTextLines.set(file.first().readText())
                    }
                }
            }
            // text box to preview input
            textarea(inputTextLines) {
                promptText = "Enter a list of inputs to fill in the prompt (separated by line)."
                hgrow = Priority.ALWAYS
                isWrapText = true
                prefWidth = 0.0
            }
        }
        parameters("Model Parameters") {
            with(common) {
                temperature()
                maxTokens()
            }
        }
        parameters("Scripting Options") {
            field("Input Limit") {
                slider(1..1000, lineLimit)
                label(lineLimit.asString())
            }
        }
    }

    private fun inputs(): List<String> {
        var split = inputTextLines.value.split("\n\n")
        if (split.size < 5)
            split = inputTextLines.value.split("\n")
        return split.filter(filter()).take(lineLimit.value)
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
                    println("Invalid regex: ${x.message}")
                    return { false }
                }
            }
        }
    }

    private fun llmFilter(prompt: String, input: String): Boolean {
        val result = runBlocking {
            AiPrompt(prompt).fill("input" to input)
                .let { completionEngine.complete(it, tokens = common.maxTokens.value, temperature = common.temp.value) }
                .value
        }
        return result?.contains("yes", ignoreCase = true) ?: false
    }

    override fun plan() = aitask("text-completion") {
        val result = mutableListOf<String>()
        inputs().forEach { line ->
            AiPrompt(template.value).fill("input" to line).let {
                completionEngine.complete(it, tokens = common.maxTokens.value, temperature = common.temp.value)
                    .value?.let { result.add(it) }
            }
        }
        AiTaskResult.result(result)
    }.task("process-results") {
        val countEach = it.groupingBy { it.cleanedup() }.eachCount()
        "Unique Results: ${countEach.size}\n" +
        "------------------\n" +
        countEach.entries.joinToString("\n") { "${it.key}: ${it.value}" } + "\n\n" +
        "All Results:\n" +
        "------------------\n" +
        it.joinToString("\n")
    }.planner

    private fun String.cleanedup() = lowercase().removeSuffix(".")

}
