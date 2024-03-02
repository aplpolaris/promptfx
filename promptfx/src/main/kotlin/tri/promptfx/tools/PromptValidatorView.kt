/*-
 * #%L
 * promptfx-0.2.3-SNAPSHOT
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

import javafx.beans.property.SimpleStringProperty
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import tornadofx.*
import tri.ai.pips.aitask
import tri.promptfx.AiPlanTaskView
import tri.promptfx.ui.EditablePromptUi
import tri.util.ui.NavigableWorkspaceViewImpl

/** Plugin for the [PromptValidatorView]. */
class PromptValidatorPlugin : NavigableWorkspaceViewImpl<PromptValidatorView>("Tools", "Prompt Validator", PromptValidatorView::class)

/** View with a prompt for testing, and a second prompt for validation. */
class PromptValidatorView : AiPlanTaskView(
    "Prompt Validator",
    "Validate a prompt against a second prompt",
) {

    val prompt = SimpleStringProperty("")
    val promptOutput = SimpleStringProperty("")
    val validatorOutput = SimpleStringProperty("")
    private lateinit var validatorPromptUi: EditablePromptUi

    init {
        addInputTextArea(prompt) {
            promptText = "Enter a prompt to validate"
        }
        input {
            validatorPromptUi = EditablePromptUi(PROMPT_VALIDATE_PREFIX, "Prompt to validate the result:")
            add(validatorPromptUi)
        }
        parameters("Model Parameters") {
            with(common) {
                temperature()
                maxTokens()
            }
        }

        outputPane.clear()
        output {
            textarea(promptOutput) {
                promptText = "Prompt output will be shown here"
                isEditable = false
                isWrapText = true
                font = Font("Segoe UI Emoji", 18.0)
                vgrow = Priority.ALWAYS
            }
            textarea(validatorOutput) {
                promptText = "Validator output will be shown here"
                isEditable = false
                isWrapText = true
                font = Font("Segoe UI Emoji", 18.0)
                vgrow = Priority.ALWAYS
            }
        }
        onCompleted {
            validatorOutput.set(it.finalResult.toString())
        }
    }

    override fun plan() = aitask("complete-prompt") {
        completionEngine.complete(prompt.value, common.maxTokens.value, common.temp.value)
    }.aitask("validate-result") {
        runLater { promptOutput.value = it }
        completionEngine.complete(validatorPromptUi.fill("result" to it), common.maxTokens.value, common.temp.value)
    }.planner

    companion object {
        private const val PROMPT_VALIDATE_PREFIX = "prompt-validate"
    }
}
