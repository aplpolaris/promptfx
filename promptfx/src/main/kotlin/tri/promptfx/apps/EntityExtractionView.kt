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

import javafx.beans.property.SimpleStringProperty
import tornadofx.combobox
import tornadofx.field
import tri.ai.openai.templatePlan
import tri.promptfx.AiPlanTaskView
import tri.promptfx.ui.promptfield
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.yaml

/** Plugin for [EntityExtractionView]. */
class EntityExtractionPlugin : NavigableWorkspaceViewImpl<EntityExtractionView>("Text", "Entity Extraction", EntityExtractionView::class)

/** View for prompts designed to extract entities from text. */
class EntityExtractionView: AiPlanTaskView("Entity Extraction", "Enter text to extract entities or facts.") {

    private val modeOptions = resources.yaml("resources/modes.yaml")["entities"] as List<String>
    private val formatModeOptions = resources.yaml("resources/modes.yaml")["structured-format"] as Map<String, String>
    private val sourceText = SimpleStringProperty("")
    private val mode = SimpleStringProperty(modeOptions[0])
    private val formatMode = SimpleStringProperty(formatModeOptions.keys.first())

    init {
        addInputTextArea(sourceText)
        parameters("Extraction Mode") {
            field("Mode") {
                combobox(mode, modeOptions) { isEditable = true }
            }
            field("Format as") {
                combobox(formatMode, formatModeOptions.keys.toList()) { isEditable = true }
            }
            promptfield(promptId = "entity-extraction", workspace = workspace)
        }
        addDefaultTextCompletionParameters(common)
    }

    override fun plan() = completionEngine.templatePlan("entity-extraction",
        "input" to sourceText.get(),
        "mode" to mode.value,
        "format" to formatModeOptions[formatMode.value]!!,
        tokenLimit = common.maxTokens.value!!,
        temp = common.temp.value
    )

}
