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
package tri.promptfx.apps

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.openai.templatePlan
import tri.promptfx.AiPlanTaskView
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.yaml

/** Plugin for the [TextToJsonView]. */
class TextToJsonPlugin : NavigableWorkspaceViewImpl<TextToJsonView>("Text", "Text-to-JSON", TextToJsonView::class)

/** View designed to convert text to JSON. */
class TextToJsonView: AiPlanTaskView("Text-to-JSON",
    "Enter text in the top box to convert to JSON (or other structured format).",) {

    private val sourceText = SimpleStringProperty("")

    private val formatModeOptions = resources.yaml("resources/modes.yaml")["structured-format"] as Map<String, String>
    private val guidance = SimpleStringProperty("")
    private val formatMode = SimpleStringProperty(formatModeOptions.keys.first())
    private val requestJson = SimpleBooleanProperty()

    private val sampleOutput = SimpleStringProperty("")

    init {
        addInputTextArea(sourceText)
        input {
            label("Sample JSON (YAML, XML, CSV, ...):")
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
                combobox(formatMode, formatModeOptions.keys.toList())
            }
        }
        parameters("Model Parameters") {
            with (common) {
                temperature()
                maxTokens()
            }
            field("Response Format") {
                tooltip("Important: when using JSON mode, you must also instruct the model to produce JSON yourself via a system or user message.")
                checkbox("JSON (if supported)", requestJson)
            }
        }
    }

    override fun plan() = completionEngine.templatePlan("text-to-json",
        "input" to sourceText.get(),
        "guidance" to guidance.get(),
        "format" to formatModeOptions[formatMode.value]!!,
        "example" to sampleOutput.get(),
        tokenLimit = common.maxTokens.value,
        temp = common.temp.value,
        requestJson = if (requestJson.value) true else null
    )

}
