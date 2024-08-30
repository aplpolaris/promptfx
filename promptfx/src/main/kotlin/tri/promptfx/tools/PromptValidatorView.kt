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

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import tornadofx.*
import tri.ai.openai.promptTask
import tri.ai.pips.aitask
import tri.ai.prompt.trace.AiPromptTrace
import tri.promptfx.AiPlanTaskView
import tri.promptfx.PromptResultArea
import tri.promptfx.ui.EditablePromptUi
import tri.util.ui.NavigableWorkspaceViewImpl

/** Plugin for the [PromptValidatorView]. */
class PromptValidatorPlugin : NavigableWorkspaceViewImpl<PromptValidatorView>("Tools", "Prompt Validator", type = PromptValidatorView::class)

/** View with a prompt for testing, and a second prompt for validation. */
class PromptValidatorView : AiPlanTaskView(
    "Prompt Validator",
    "Validate text completion output using a secondary prompt."
) {

    val prompt = SimpleStringProperty("")
    private val promptOutput = SimpleObjectProperty<AiPromptTrace>()
    private val validatorOutput = SimpleObjectProperty<AiPromptTrace>()
    private lateinit var validatorPromptUi: EditablePromptUi

    init {
        addInputTextArea(prompt) {
            promptText = "Enter a prompt whose response you want to validate"
        }
        input {
            validatorPromptUi = EditablePromptUi(PROMPT_VALIDATE_PREFIX, "Prompt to validate the result:")
            add(validatorPromptUi)
        }
        addDefaultTextCompletionParameters(common)

        outputPane.clear()
        output {
            add(PromptResultArea().apply {
                promptOutput.onChange { setFinalResult(it!!) }
            })
            add(PromptResultArea().apply {
                validatorOutput.onChange { setFinalResult(it!!) }
            })
        }
        onCompleted {
            validatorOutput.set(it.finalResult!!.first() as AiPromptTrace)
        }
    }

    override fun plan() = aitask("complete-prompt") {
        completionEngine.promptTask(prompt.value, common.maxTokens.value, common.temp.value)
    }.aitask("validate-result") {
        runLater { promptOutput.value = it }
        val validatorPromptText = validatorPromptUi.fill("result" to it.outputInfo.outputs!!.first().toString())
        completionEngine.promptTask(validatorPromptText, common.maxTokens.value, common.temp.value, numResponses = common.numResponses.value)
    }.planner

    companion object {
        private const val PROMPT_VALIDATE_PREFIX = "prompt-validate"
    }
}
