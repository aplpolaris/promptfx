/*-
 * #%L
 * promptfx-0.1.8
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
package tri.promptfx.tools

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.ScrollPane
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.pips.aitask
import tri.ai.prompt.AiPrompt
import tri.ai.prompt.AiPromptLibrary
import tri.promptfx.AiPlanTaskView
import tri.promptfx.CommonParameters
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.slider
import java.time.LocalDate

/** Plugin for the [PromptTemplateView]. */
class PromptTemplatePlugin : NavigableWorkspaceViewImpl<PromptTemplateView>("Tools", "Prompt Template", PromptTemplateView::class)

/** A view designed to help you test prompt templates. */
class PromptTemplateView : AiPlanTaskView("Prompt Template",
    "Enter a prompt template and a list of values to fill it in with.") {

    private val template = SimpleStringProperty("")
    private val fields = observableListOf<Pair<String, String>>()
    private val fieldMap = mutableMapOf<String, String>()

    private val common = CommonParameters()
    private val maxTokens = SimpleIntegerProperty(500)

    init {
        template.onChange { updateTemplateInputs(it!!) }
    }

    init {
        input {
            spacing = 10.0
            paddingAll = 10.0
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
                promptText = "Enter a prompt template"
                hgrow = Priority.ALWAYS
                prefRowCount = 20
                isWrapText = true
                prefWidth = 0.0
            }
        }
        input {
            spacing = 10.0
            paddingAll = 10.0
            text("Inputs:")
            listview(fields) {
                vgrow = Priority.ALWAYS
                cellFormat { field ->
                    graphic = hbox {
                        spacing = 10.0
                        alignment = Pos.TOP_CENTER
                        text(field.first)
                        val useText = field.second.ifBlank {
                            if (field.first == "today") LocalDate.now().toString() else ""
                        }
                        fieldMap[field.first] = useText
                        val area = textarea(useText) {
                            isWrapText = true
                            hgrow = Priority.ALWAYS
                            promptText = "Enter value for ${field.first}"
                            prefRowCount = 0
                            textProperty().onChange { fieldMap[field.first] = it!! }
                        }
                        // add button to toggle expanding the text area
                        button("", FontAwesomeIconView(FontAwesomeIcon.EXPAND)) {
                            action {
                                area.prefRowCount = when (area.prefRowCount) {
                                    0 -> 5
                                    5 -> 10
                                    else -> 0
                                }
                            }
                        }
                        prefWidth = 0.0
                    }
                }
            }
        }
        parameters("Parameters") {
            with(common) {
                temperature()
                field("Max tokens") {
                    tooltip("Max # of tokens for combined query/response from the question answering engine")
                    slider(1..2000, maxTokens)
                    label(maxTokens)
                }
            }
        }
    }

    override fun plan() = aitask("text-completion") {
        AiPrompt(template.value).fill(fieldMap).let {
            completionEngine.complete(it, temperature = common.temp.value, tokens = maxTokens.value)
        }
    }.planner

    private fun updateTemplateInputs(template: String) {
        // extract {{{.}}} and {{.}} delimited fields from new value
        var templateText = template
        val nueFields = templateText.split("{{{").drop(1).map { it.substringBefore("}}}") }.toMutableSet()
        nueFields.forEach { templateText = templateText.replace("{{{$it}}}", "") }
        nueFields.addAll(templateText.split("{{").drop(1).map { it.substringBefore("}}") })
        nueFields.removeIf { it.isBlank() || it[0] in "/#^" }
        if (fields.toSet() != nueFields.toSet()) {
            val fieldMapCopy = fieldMap.toMap()
            fieldMap.clear()
            fields.setAll(nueFields.map { it to (fieldMapCopy[it] ?: "") })
        }
    }

}
