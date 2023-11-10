package tri.promptfx.ui

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.prompt.AiPromptLibrary

/** View for selecting and editing a prompt. */
class EditablePromptUi(val prefix: String, val instruction: String): Fragment() {

    private val prompts
        get() = AiPromptLibrary.withPrefix(prefix)
    val templateText = SimpleStringProperty(prompts.firstOrNull()?.let { AiPromptLibrary.lookupPrompt(it).template } ?: "")

    override val root = vbox {
        hbox {
            alignment = Pos.CENTER_LEFT
            spacing = 5.0
            text(instruction)
            spacer()
            menubutton("", FontAwesomeIconView(FontAwesomeIcon.LIST)) {
                prompts.forEach { key ->
                    item(key) {
                        action { templateText.set(AiPromptLibrary.lookupPrompt(key).template) }
                    }
                }
            }
        }
        textarea(templateText) {
            vgrow = Priority.ALWAYS
            isWrapText = true
            style = "-fx-font-size: 18px;"
        }
    }

}