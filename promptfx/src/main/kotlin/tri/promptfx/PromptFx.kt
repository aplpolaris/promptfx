package tri.promptfx

import javafx.scene.Cursor
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import tornadofx.*

class PromptFx : App(PromptFxWorkspace::class, PromptFxStyles::class)

fun main(args: Array<String>) {
    launch<PromptFx>(args)
}

/** Stylesheet for the application. */
class PromptFxStyles: Stylesheet() {
    companion object {
        val transparentTextArea by cssclass()
        val scrollPane by cssclass()
        val content by cssclass()
    }

    init {
        transparentTextArea {
            cursor = Cursor.TEXT
            textFill = Color.WHITE
            fontFamily = "Serif"
            textAlignment = TextAlignment.CENTER

            scrollPane {
                content {
                    backgroundColor += Color.TRANSPARENT
                    borderColor += box(Color.TRANSPARENT)
                }
            }
        }
    }
}