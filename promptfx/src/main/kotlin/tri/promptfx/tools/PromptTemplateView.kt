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
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.pips.aitask
import tri.ai.prompt.AiPrompt
import tri.ai.prompt.trace.*
import tri.promptfx.AiPlanTaskView
import tri.promptfx.PromptFxModels
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.templatemenubutton
import tri.util.warning
import java.time.LocalDate

/** Plugin for the [PromptTemplateView]. */
class PromptTemplatePlugin : NavigableWorkspaceViewImpl<PromptTemplateView>("Tools", "Prompt Template", type = PromptTemplateView::class)

/** A view designed to help you test prompt templates. */
class PromptTemplateView : AiPlanTaskView("Prompt Template",
    "Enter a prompt template and a list of values to fill it in with.") {

    val template = SimpleStringProperty("")
    private val fields = observableListOf<Pair<String, String>>()
    private val fieldMap = mutableMapOf<String, String>()

    init {
        template.onChange { updateTemplateInputs(it!!) }
    }

    init {
        input(5, 5, Priority.ALWAYS) {
            hbox(5, Pos.CENTER_LEFT) {
                text("Template:")
                spacer()
                templatemenubutton(template)
            }
            textarea(template) {
                promptText = "Enter a prompt template, using syntax like {{field}} for fields to fill in."
                hgrow = Priority.ALWAYS
                prefRowCount = 20
                isWrapText = true
                prefWidth = 0.0
            }
        }
        input(10, 10) {
            text("Inputs:")
            listview(fields) {
                vgrow = Priority.ALWAYS
                cellFormat { field ->
                    graphic = hbox(10, Pos.TOP_CENTER) {
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
        addDefaultTextCompletionParameters(common)
    }

    override fun plan() = aitask("text-completion") {
        val resp = AiPrompt(template.value).fill(fieldMap).let {
            completionEngine.complete(it, tokens = common.maxTokens.value, temperature = common.temp.value)
        }
        resp.mapvalue {
            AiPromptTrace(
                AiPromptInfo(template.value, fieldMap.toMap()),
                AiPromptModelInfo(completionEngine.modelId, common.toModelParams()),
                AiPromptExecInfo(resp.errorMessage, responseTimeMillis = resp.durationTotal?.toMillis()),
                AiPromptOutputInfo(it)
            )
        }
    }.planner

    /**
     * Loads a prompt trace into the view.
     * Will set the prompt, prompt inputs, model, and model parameters associated with the trace.
     */
    fun importPromptTrace(prompt: AiPromptTrace) {
        template.set(prompt.promptInfo.prompt)
        fields.setAll(prompt.promptInfo.promptParams.entries.map { it.key to it.value.toString() })
        val model = PromptFxModels.textCompletionModels().find { it.modelId == prompt.modelInfo.modelId }
        if (model != null) {
            controller.completionEngine.set(model)
        } else {
            warning<PromptTemplateView>("Model ${prompt.modelInfo.modelId} not found.")
        }
        common.importModelParams(prompt.modelInfo.modelParams)
    }

    private fun updateTemplateInputs(template: String) {
        // extract {{{.}}} and {{.}} delimited fields from new value
        val nueFields = AiPrompt.fieldsInTemplate(template)
        if (fields.toSet() != nueFields.toSet()) {
            val fieldMapCopy = fieldMap.toMap()
            fieldMap.clear()
            fields.setAll(nueFields.map { it to (fieldMapCopy[it] ?: "") })
        }
    }

}
