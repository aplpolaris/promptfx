/*-
 * #%L
 * promptfx-0.1.0-SNAPSHOT
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
package tri.promptfx.apps

import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.openai.instructTextPlan
import tri.ai.prompt.AiPromptLibrary
import tri.promptfx.AiPlanTaskView
import tri.util.ui.NavigableWorkspaceViewImpl

/** Plugin for the [QuestionAnsweringView]. */
class QuestionAnsweringPlugin : NavigableWorkspaceViewImpl<QuestionAnsweringView>("Text", "Question Answering", QuestionAnsweringView::class)

/** View with prompts designed to answer questions. */
class QuestionAnsweringView: AiPlanTaskView("Question Answering",
    "Enter question in the top box, and the text with an answer in the box below.",) {

    private val question = SimpleStringProperty("")
    private val sourceText = SimpleStringProperty("")

    private val promptId = SimpleStringProperty("question-answer")
    private val promptIdList = AiPromptLibrary.INSTANCE.prompts.keys.filter { it.startsWith("question-answer") }

    private val promptText = promptId.stringBinding { AiPromptLibrary.lookupPrompt(it!!).template }

    init {
        addInputTextArea(question)
        input {
            label("Source Text:")
            textarea(sourceText) {
                isWrapText = true
            }
        }
        parameters("Prompt Template") {
            field("Template") {
                combobox(promptId, promptIdList)
            }
            field(null, forceLabelIndent = true) {
                text(promptText).apply {
                    wrappingWidth = 300.0
                    promptText.onChange { tooltip(it) }
                }
            }
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
        instruct = question.get(),
        userText = sourceText.get(),
        tokenLimit = common.maxTokens.value!!,
        temp = common.temp.value,
    )

}