package tri.promptfx

import javafx.beans.property.SimpleStringProperty
import tri.ai.openai.promptPlan

/** View that provides a single input box and combines that with a prompt from the prompt library. */
open class AiCompletionView(
    title: String,
    description: String,
    val promptId: String,
    val tokenLimit: Int,
    val stop: String? = null
) : AiPlanTaskView(title, description) {

    private val input = SimpleStringProperty("")

    init {
        addInputTextArea(input)
    }

    override fun plan() = completionEngine.promptPlan(promptId, input.get(), tokenLimit, stop)

}