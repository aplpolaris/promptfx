package tri.promptfx

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.control.TextInputDialog
import tornadofx.*
import tri.ai.core.TextPlugin
import tri.ai.openai.OpenAiClient
import tri.util.ui.graphic

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
