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
package tri.util.ui

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.TextField
import javafx.stage.Screen
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.promptfx.PromptFxController
import tri.promptfx.PromptFxDriver.sendInput
import tri.promptfx.PromptFxModels
import tri.promptfx.PromptFxWorkspace
import tri.promptfx.docs.DocumentQaView
import tri.promptfx.docs.DocumentQaView.Companion.browseToBestSnippet
import tri.ai.text.docs.FormattedText
import tri.promptfx.ui.toFxNodes
import tri.util.ui.DocumentUtils.documentThumbnail

/** View for a full-screen chat display. */
class ImmersiveChatView : Fragment("Immersive Chat") {

    val baseComponentTitle: String? by param()
    val baseComponent: View? by param()
    val inputFontSize = SimpleIntegerProperty(48)

    val indicator = BlinkingIndicator(FontAwesomeIcon.ROCKET).also {
        it.glyphSize = 60.0
        it.glyphStyle = "-fx-fill: white;"
    }

    val controller: PromptFxController by inject()
    val input = SimpleStringProperty("")

    private lateinit var inputField: TextField
    private lateinit var output: AnimatingTextFlow
    private lateinit var thumbnails: AnimatingThumbnailBox

    val css = ImmersiveChatView::class.java.getResource("resources/chat.css")!!

    init {
        if (baseComponent is DocumentQaView) {
            val base = baseComponent as DocumentQaView
            base.snippets.onChange {
                val thumbs = base.snippets.map { it.document.browsable()!! }.toSet()
                    .take(MAX_THUMBNAILS_FOR_IMMERSIVE_CHAT_VIEW)
                    .map { DocumentThumbnail(it, documentThumbnail(it, DOC_THUMBNAIL_SIZE, false)) }
                thumbnails.animateThumbs(thumbs)
            }
        }
    }

    override val root = vbox {
        alignment = Pos.CENTER
        spacing = 20.0
        stylesheets.add(css.toExternalForm())

        val curScreen = Screen.getScreensForRectangle(primaryStage.x, primaryStage.y, 1.0, 1.0).firstOrNull()
            ?: Screen.getPrimary()
        val screenHeight = curScreen.bounds.height

        // add spacer with height 0.1
        vbox {
            prefHeight = 0.1 * screenHeight
            padding = insets(10.0, 0.0, 0.0, 0.0)
            hbox(50.0) {
                alignment = Pos.CENTER
                addPolicyBox()
                indicator.attachTo(this)
                addPolicyBox()
            }
        }

        text("You are in: ${baseComponentTitle ?: "Test"} Mode") {
            alignment = Pos.CENTER
            prefHeight = 0.07 * screenHeight
            style = "-fx-font-size: 24px; -fx-fill: gray; -fx-font-weight: bold;"
        }

        inputField = textfield(input) {
            id = "chat-input"
            prefHeight = 0.15 * screenHeight
            alignment = Pos.CENTER
            action { handleUserAction { } }
            inputFontSize.onChange {
                style = "-fx-font-size: ${it}px;"
            }
            // when pressing up or down adjust the font size
            setOnKeyPressed {
                when (it.code) {
                    javafx.scene.input.KeyCode.UP -> inputFontSize.value += 2
                    javafx.scene.input.KeyCode.DOWN -> inputFontSize.value -= 2
                    else -> {}
                }
            }
        }

        output = AnimatingTextFlow().apply {
            root.prefHeight = 0.3 * screenHeight
            root.alignment = Pos.CENTER
        }
        add(output)

        val action: ((DocumentThumbnail) -> Unit)? = when (val view = baseComponent) {
            is DocumentQaView ->
                { doc -> browseToBestSnippet(doc.document, view.planner.lastResult, hostServices) }
            else -> null
        }
        thumbnails = AnimatingThumbnailBox(action).apply {
            alignment = Pos.CENTER
            prefHeight = 0.22 * screenHeight
            spacing = 40.0
        }
        add(thumbnails)

        vbox {
            prefHeight = 0.02 * screenHeight
            padding = insets(10.0, 0.0, 0.0, 0.0)
        }
    }

    //region INPUT/OUTPUT

    internal fun setUserInput(text: String, callback: (FormattedText) -> Unit) {
        input.set(text)
        handleUserAction(callback)
    }

    private fun handleUserAction(callback: (FormattedText) -> Unit) {
        runLater {
            output.textNodes.setAll()
            indicator.startBlinking()
        }
        runAsync {
            runBlocking {
                (workspace as PromptFxWorkspace).sendInput(baseComponentTitle!!, input.value, callback)
            }
        } ui {
            indicator.stopBlinking()
            controller.updateUsage()
            output.animateText(it.toFxNodes(), onFinished = {
                (root.scene.lookup("#chat-input") as TextField).selectAll()
            })
        }
    }

    //endregion

    companion object {
        private const val DOC_THUMBNAIL_SIZE = 240
        private const val MAX_THUMBNAILS_FOR_IMMERSIVE_CHAT_VIEW = 8
    }

}

/** Add a box associated with the global model access policy. */
internal fun EventTarget.addPolicyBox() {
    if (!PromptFxModels.policy.isShowBanner) return
    label(PromptFxModels.policy.bar.text) {
        padding = insets(0.0, 5.0, 0.0, 5.0)
        alignment = Pos.CENTER
        style {
            fontSize = 24.px
            fontWeight = javafx.scene.text.FontWeight.BOLD
            fill = PromptFxModels.policy.bar.fgColorDark
            backgroundColor += PromptFxModels.policy.bar.bgColorDark
            borderRadius += box(10.px)
            backgroundRadius += box(10.px)
        }
    }
}
