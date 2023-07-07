package tri.promptfx.apps

import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.openai.instructTextPlan
import tri.promptfx.AiPlanTaskView
import tri.util.ui.NavigableWorkspaceViewImpl

class QuestionAnsweringView: AiPlanTaskView("Question Answering",
    "Enter question in the top box, and the text with an answer in the box below.",) {

    private val question = SimpleStringProperty("")
    private val sourceText = SimpleStringProperty("")

    init {
        addInputTextArea(question)
        input {
            label("Source Text:")
            textarea(sourceText) {
                isWrapText = true
            }
        }
    }

    override fun plan() = completionEngine.instructTextPlan(
        "question-answer",
        instruct = question.get(),
        userText = sourceText.get(),
        tokenLimit = 500)

}

class QuestionAnsweringPlugin : NavigableWorkspaceViewImpl<QuestionAnsweringView>("Text", "Question Answering", QuestionAnsweringView::class)