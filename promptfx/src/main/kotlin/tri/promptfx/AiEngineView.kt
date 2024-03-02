/*-
 * #%L
 * promptfx-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
package tri.promptfx

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.control.TextInputDialog
import tornadofx.*
import tri.ai.core.TextPlugin
import tri.ai.openai.OpenAiClient
import tri.promptfx.PromptFxDriver.showDriverDialog
import tri.util.ui.graphic

/** View for selecting which model to use, and for picking between models. */
class AiEngineView: View() {

    val controller: PromptFxController by inject()

    override val root = hbox {
        alignment = Pos.CENTER_LEFT
        spacing = 10.0

        label("Completions: ") {
            style = "-fx-font-weight: bold;"
        }
        with (controller) {
            combobox(completionEngine, TextPlugin.textCompletionModels()) {
                tooltip("The default completion engine to use for all text completion tasks, if not selectable in view parameters.")
                maxWidth = 200.0
            }
            label("Embeddings: ") {
                style = "-fx-font-weight: bold;"
            }
            combobox(embeddingService, TextPlugin.embeddingModels()) {
                tooltip("The default embedding engine to use for all text embedding tasks, if not selectable in view parameters.")
                maxWidth = 200.0
            }
            button("", graphic = FontAwesomeIcon.KEY.graphic) {
                action {
                    TextInputDialog(OpenAiClient.INSTANCE.settings.apiKey).apply {
                        initOwner(primaryStage)
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
                tooltip("Estimated usage of tokens and images. The actual value may be higher. Click to navigate to the OpenAI account usage page.")
                style = "-fx-font-weight: bold;"
                cursor = Cursor.HAND
                onLeftClick {
                    hostServices.showDocument("https://beta.openai.com/account/usage")
                }
            }

            // button for testing view driver
            button("", graphic = FontAwesomeIcon.COG.graphic) {
                isManaged = false
                isVisible = false
                action { showDriverDialog() }
            }
        }
    }

}
