/*-
 * #%L
 * promptfx-0.1.0-SNAPSHOT
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
import tri.ai.openai.instructTextPlan
import tri.promptfx.*
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.yaml

/** Plugin for the [TranslationView]. */
class TranslationPlugin : NavigableWorkspaceViewImpl<TranslationView>("Text", "Translation", TranslationView::class)

/** View designed to automatically translate text. */
class TranslationView: AiPlanTaskView("Translation", "Enter text to translate.") {

    private val modeOptions = resources.yaml("resources/modes.yaml")["translation"] as List<String>
    private val mode = SimpleStringProperty(modeOptions[0])
    private val sourceText = SimpleStringProperty("")

    init {
        addInputTextArea(sourceText)
        parameters("Target Language") {
            field("Language") {
                combobox(mode, modeOptions)
            }
        }
        parameters("Model Parameters") {
            with (common) {
                temperature()
                maxTokens()
            }
        }
    }

    override fun plan() = completionEngine.instructTextPlan("translate-text",
        instruct = mode.get(),
        userText = sourceText.get(),
        tokenLimit = common.maxTokens.value,
        temp = common.temp.value,
    )

}
