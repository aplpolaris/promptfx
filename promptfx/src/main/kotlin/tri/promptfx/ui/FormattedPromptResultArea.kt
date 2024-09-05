package tri.promptfx.ui

import javafx.scene.input.DataFormat
import javafx.scene.layout.Priority
import javafx.scene.text.TextFlow
import tornadofx.*
import tri.util.ui.plainText

/** Formatted text area for displaying a prompt result or other output, based on [TextFlow].
 * Adds support for clickable links, cycling outputs if multiple, and save to file.
 */
class FormattedPromptResultArea : PromptResultAreaSupport("Formatted Prompt Result Area") {

    private val htmlArea = TextFlow()

    override val root = vbox {
        vgrow = Priority.ALWAYS
        addtoolbar()
        scrollpane {
            vgrow = Priority.ALWAYS
            isFitToWidth = true
            htmlArea.apply {
                padding = insets(5.0)
                vgrow = Priority.ALWAYS
                style = "-fx-font-size: 16px;"

                promptTraceContextMenu(this@FormattedPromptResultArea, trace) {
                    item("Copy output to clipboard") {
                        action {
                            clipboard.setContent(mapOf(
                                DataFormat.HTML to selectionHtml.value,
                                DataFormat.PLAIN_TEXT to plainText()
                            ))
                        }
                    }
                }
            }
            this += htmlArea
        }
    }

    init {
        root
        selectionFormatted.onChange {
            htmlArea.children.clear()
            htmlArea.children.addAll(it?.toFxNodes() ?: listOf())
        }
    }

}