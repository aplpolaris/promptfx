/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx.text

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.pips.taskPlan
import tri.ai.prompt.PromptTemplate
import tri.promptfx.AiPlanTaskView
import tri.promptfx.PromptFxGlobals.lookupPrompt
import tri.promptfx.RuntimePromptViewConfigs
import tri.promptfx.ui.PromptSelectionModel
import tri.promptfx.ui.promptfield
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance

/** Plugin for the [StructuredDataView]. */
class StructuredDataPlugin : NavigableWorkspaceViewImpl<StructuredDataView>("Text", "Structured Data", WorkspaceViewAffordance.INPUT_ONLY, StructuredDataView::class)

/** View designed to convert text to JSON. */
class StructuredDataView: AiPlanTaskView("Structured Data",
    "Enter text in the top box to convert to JSON or another structured format.") {

    private val sourceText = SimpleStringProperty("")

    private val formatModeOptions = RuntimePromptViewConfigs.modeOptionMap("structured-format")
    private val guidance = SimpleStringProperty("")
    private val formatMode = SimpleStringProperty(formatModeOptions.keys.first())
    private val requestJson = SimpleBooleanProperty()

    private val sampleOutput = SimpleStringProperty("")

    init {
        addInputTextArea(sourceText)
        input {
            toolbar {
                text("Sample JSON (YAML, XML, CSV, ...):")
            }
            textarea(sampleOutput) {
                isWrapText = true
            }
        }
        parameters("Extraction Mode") {
            field("Guidance") {
                tooltip("If this is not blank, adds 'The result should contain X' to the instruction.")
                textfield(guidance)
            }
            field("Format as") {
                combobox(formatMode, formatModeOptions.keys.toList()) { isEditable = true }
            }
            promptfield(prompt = PromptSelectionModel("text-extract/text-to-json"), workspace = workspace)
        }
        addDefaultChatParameters(common)
        parameters("Response") {
            field("Response Format") {
                tooltip("Important: when using JSON mode, you must also instruct the model to produce JSON yourself via a system or user message.")
                checkbox("JSON (if supported)", requestJson)
            }
        }
    }

    override fun plan() = common.completionBuilder()
        .prompt(lookupPrompt("text-extract/text-to-json"))
        .params(PromptTemplate.INPUT to sourceText.get(), "guidance" to guidance.get(), "format" to format(), "example" to sampleOutput.get())
        .requestJson(if (requestJson.value) true else null)
        .taskPlan(chatEngine)

    private fun format() = formatModeOptions[formatMode.value] ?: formatMode.value

}
