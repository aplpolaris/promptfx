package tri.promptfx.`fun`

import javafx.beans.property.SimpleStringProperty
import javafx.scene.paint.Color
import tri.ai.openai.promptPlan
import tri.promptfx.AiPlanTaskView

/** View to approximate a color based on user text. */
class ColorView : AiPlanTaskView("Colors", "Enter a description of a color or object to generate a color.") {

    private val input = SimpleStringProperty("")

    init {
        addInputTextArea(input)
        onCompleted {
            updateOutputTextAreaColor(it.finalResult.toString())
        }
    }

    private fun updateOutputTextAreaColor(result: String) {
        val outputEditor = outputPane.lookup(".text-area") as javafx.scene.control.TextArea
        try {
            val color = Color.web(result.trim())
            val fgColor = if (color.brightness > 0.5) "#000000" else "#ffffff"
            // update outputEditor foreground and background colors based on this
            outputEditor.lookup(".content").style = "-fx-background-color: ${color.hex()};"
            outputEditor.style = "-fx-text-fill: $fgColor;"
        } catch (x: IllegalArgumentException) {
            println("Failed to switch color. Result was $result.")
        }
    }

    override fun plan() = completionEngine.promptPlan("example-color", input.get(), tokenLimit = 6, stop = ";")

    private fun Color.hex() = "#${this.toString().substring(2, 8)}"

}
