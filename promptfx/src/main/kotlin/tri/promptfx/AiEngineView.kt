package tri.promptfx

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.control.TextInputDialog
import tornadofx.*
import tri.ai.core.TextPlugin
import tri.ai.openai.OpenAiClient
import tri.util.ui.graphic

/** Simple view that aggregates tokens used. */
class AiEngineView: View() {

    val controller: PromptFxController by inject()

    override val root = hbox {
        alignment = Pos.CENTER_LEFT
        spacing = 10.0

        label("Completion Engine: ") {
            style = "-fx-font-weight: bold;"
        }
        with (controller) {
            combobox(completionEngine, TextPlugin.textCompletionModels())
            button("", graphic = FontAwesomeIcon.KEY.graphic) {
                action {
                    TextInputDialog(OpenAiClient.INSTANCE.settings.apiKey).apply {
                        title = "OpenAI API Key"
                        headerText = "Enter your OpenAI API key."
                        contentText = "API Key:"
                        showAndWait().ifPresent {
                            if (it.isNotBlank())
                                OpenAiClient.INSTANCE.settings.apiKey = it
                        }
                    }
                }
            }
            label(tokensUsed.stringBinding(audioUsed, imagesUsed) {
                "Usage: $it tokens, ${audioUsed.value} audio, ${imagesUsed.value} images"
                    .replace(", 0 audio", "")
                    .replace(", 0 images", "")
            }) {
                style = "-fx-font-weight: bold;"
                cursor = Cursor.HAND
                onLeftClick {
                    hostServices.showDocument("https://beta.openai.com/account/usage")
                }
            }
        }
    }

}