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
import tornadofx.*
import tri.ai.openai.instructTextPlan
import tri.ai.prompt.AiPromptLibrary
import tri.promptfx.AiPlanTaskView
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.promptfield

/** Plugin for the [QuestionAnsweringView]. */
class QuestionAnsweringPlugin : NavigableWorkspaceViewImpl<QuestionAnsweringView>("Text", "Question Answering", QuestionAnsweringView::class)

/** View with prompts designed to answer questions. */
class QuestionAnsweringView: AiPlanTaskView("Question Answering",
    "Enter question in the top box, and the text with an answer in the box below.",) {

    companion object {
        private const val PROMPT_PREFIX = "question-answer"
    }

    private val instruct = SimpleStringProperty("")
    private val input = SimpleStringProperty("")

    private val promptId = SimpleStringProperty(PROMPT_PREFIX)
    private val promptText = promptId.stringBinding { AiPromptLibrary.lookupPrompt(it!!).template }

    init {
        addInputTextArea(instruct) {
            promptText = "Provide the question here. This will replace {{{instruct}}} in the prompt."
        }
        input {
            label("Source Text:")
            textarea(input) {
                promptText = "Provide the text here. This will replace {{{input}}} in the prompt."
                isWrapText = true
            }
        }
        parameters("Prompt Template") {
            tooltip("Loads from prompts.yaml with prefix $PROMPT_PREFIX")
            promptfield("Template", promptId, AiPromptLibrary.withPrefix(PROMPT_PREFIX), promptText, workspace)
        }
        parameters("Model Parameters") {
            with (common) {
                temperature()
                maxTokens()
            }
        }
    }

    override fun plan() = completionEngine.instructTextPlan(
        promptId.value,
        instruct = instruct.get(),
        userText = input.get(),
        tokenLimit = common.maxTokens.value!!,
        temp = common.temp.value,
    )

}
