package tri.promptfx

import javafx.scene.Node
import javafx.scene.control.TextArea
import javafx.scene.text.Text
import tri.promptfx.docs.FormattedText
import tri.promptfx.docs.toFxNodes

/** General-purpose capability for sending inputs to PromptFx views and getting a result. */
object PromptFxDriver {

    /** Send input to a named view, run, and execute a callback when the result is received. */
    suspend fun PromptFxWorkspace.sendInput(viewName: String, input: String, callback: (List<Node>) -> Unit): List<Node> {
        val taskView = findTaskView(viewName)
        val inputArea = taskView?.inputArea()

        val result = if (inputArea == null) {
            listOf(Text("No view found with name $viewName, or that view does not support input."))
        } else {
            inputArea.text = input
            val result = taskView.processUserInput()
            (result.finalResult as? FormattedText)?.toFxNodes()
                ?: listOf(Text(result.finalResult.toString()))
        }

        callback(result)
        return result
    }

}