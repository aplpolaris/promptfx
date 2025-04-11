/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
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
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.event.EventHandler
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.control.TextInputDialog
import tornadofx.*
import tri.ai.openai.OpenAiAdapter
import tri.promptfx.PromptFxDriver.showDriverDialog
import tri.util.ui.graphic

/** View for selecting which model to use, and for picking between models. */
class AiEngineView: View() {

    val controller: PromptFxController by inject()

    override val root = hbox {
        alignment = Pos.CENTER_LEFT
        spacing = 10.0

        with (controller) {
            menubutton("Completions", FontAwesomeIconView(FontAwesomeIcon.LIST)) {
                tooltip(completionEngine.value?.toString() ?: "Select the completion engine to use.")
                completionEngine.onChange { tooltip.text = it.toString() }
                PromptFxModels.textCompletionModels().forEach { model ->
                    item(model.toString()) {
                        style = menustyle(model.modelId, completionEngine.value?.modelId)
                        styleProperty().bind(completionEngine.stringBinding { menustyle(model.modelId, it?.modelId) })
                        action { completionEngine.set(model) }
                    }
                }
            }
            menubutton("Embeddings", FontAwesomeIconView(FontAwesomeIcon.LIST)) {
                tooltip(embeddingService.value?.toString() ?: "Select the embedding engine to use.")
                embeddingService.onChange { tooltip.text = it.toString() }
                PromptFxModels.embeddingModels().forEach { model ->
                    item(model.toString()) {
                        style = menustyle(model.modelId, embeddingService.value?.modelId)
                        styleProperty().bind(embeddingService.stringBinding { menustyle(model.modelId, it?.modelId) })
                        action { embeddingService.set(model) }
                    }
                }
            }

            if (PromptFxModels.policy.isShowUsage)
                usagelabel()

            if (PromptFxModels.policy.isShowApiKeyButton)
                button("", graphic = FontAwesomeIcon.KEY.graphic) {
                    action {
                        TextInputDialog(OpenAiAdapter.INSTANCE.settings.apiKey).apply {
                            initOwner(primaryStage)
                            title = "OpenAI API Key"
                            headerText = "Enter your OpenAI API key."
                            contentText = "API Key:"
                            showAndWait().ifPresent {
                                if (it.isNotBlank())
                                    OpenAiAdapter.INSTANCE.settings.apiKey = it
                            }
                        }
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

    private fun EventTarget.usagelabel() = label("", graphic = FontAwesomeIconView(FontAwesomeIcon.DOLLAR)) {
        val defStr = "Estimated usage of tokens and images. The actual value may be higher. Click to navigate to the OpenAI account usage page.\n"
        tooltip(defStr) {
            // update usage stats when the tooltip is shown
            onShowing = EventHandler {
                controller.updateUsage()
                text = defStr + "Usage: ${controller.tokensUsed.value} tokens, ${controller.audioUsed.value} audio, ${controller.imagesUsed.value} images"
                    .replace(", 0 audio", "")
                    .replace(", 0 images", "")
            }
        }
        style = "-fx-font-weight: bold;"
        cursor = Cursor.HAND
        onLeftClick {
            hostServices.showDocument("https://beta.openai.com/account/usage")
        }
    }

    /** Makes the selected menu item bold. */
    private fun menustyle(a: String, b: String?) =
        if (a == b) "-fx-font-weight: bold;" else ""

}
