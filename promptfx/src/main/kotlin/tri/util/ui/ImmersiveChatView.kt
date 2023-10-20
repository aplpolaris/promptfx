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
package tri.util.ui

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.animation.Timeline
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Hyperlink
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.Screen
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.embedding.EmbeddingDocument
import tri.promptfx.DocumentUtils.documentThumbnail
import tri.promptfx.PromptFxController
import tri.promptfx.docs.DocumentQaView
import tri.promptfx.docs.DocumentQaView.Companion.browseToBestSnippet

/** View for a full-screen chat display. */
class ImmersiveChatView : Fragment("Immersive Chat") {

    val onUserRequest: suspend (String) -> List<Node> by param()
    val baseComponentTitle: String? by param()
    val baseComponent: View? by param()
    val inputFontSize = SimpleIntegerProperty(64)

    val indicator = FontAwesomeIcon.ROCKET.graphic.also {
        it.glyphSize = 60.0
        it.glyphStyle = "-fx-fill: white;"
    }

    val controller: PromptFxController by inject()
    private val thumbnailList = observableListOf<DocumentThumbnail>()
    val input = SimpleStringProperty("")
    val response = observableListOf<Node>()

    val css = ImmersiveChatView::class.java.getResource("resources/chat.css")!!

    init {
        if (baseComponent is DocumentQaView) {
            val base = baseComponent as DocumentQaView
            base.snippets.onChange {
                val thumbs = base.snippets.map { it.embeddingMatch.document }.toSet()
                    .associateWith { documentThumbnail(it) }
                animateThumbs(thumbs)
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
            hbox {
                alignment = Pos.CENTER
                indicator.attachTo(this)
            }
        }

        text("You are in: ${baseComponentTitle ?: "Test"} Mode") {
            alignment = Pos.CENTER
            prefHeight = 0.1 * screenHeight
            style = "-fx-font-size: 24px; -fx-fill: gray; -fx-font-weight: bold;"
        }

        textfield(input) {
            id = "chat-input"
            prefHeight = 0.2 * screenHeight
            alignment = Pos.CENTER
            action { handleUserAction() }
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

        hbox {
            prefHeight = 0.3 * screenHeight
            alignment = Pos.CENTER
            scrollpane {
                id = "chat-response-scroll"
                hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
                vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
                maxHeight = Screen.getPrimary().bounds.height / 2
                hbox {
                    textflow {
                        id = "chat-response"
                        response.onChange { updateTextFlow(it) }
                        prefWidth = minOf(2000.0, Screen.getPrimary().bounds.width * 2 / 3)
                    }
                    vbox { prefWidth = 20.0 }
                }
            }
        }

        hbox {
            alignment = Pos.CENTER
            prefHeight = 0.25 * screenHeight
            spacing = 40.0
            children.bind(thumbnailList) { docthumbnail(it) }
        }
    }

    private fun TextFlow.updateTextFlow(change: ListChangeListener.Change<out Node>) {
        children.clear()
        children.addAll(change.list)
        children.filterIsInstance<Text>().forEach {
            it.styleClass += "chat-text-default"
        }
    }

    private fun handleUserAction() {
        response.setAll()
        blinkIndicator(start = true)
        runAsync {
            runBlocking { onUserRequest(input.value) }
        } ui {
            blinkIndicator(start = false)
            controller.updateUsage()
            TextFlowAnimator.animateText(it, target = response, onFrame = {
                (root.scene.lookup("#chat-response-scroll") as ScrollPane).vvalue = 1.0
            }, onFinished = {
                (root.scene.lookup("#chat-input") as TextField).selectAll()
            })
        }
    }

    //region ANIMATION

    private fun EventTarget.docthumbnail(doc: DocumentThumbnail) = vbox {
        val action: (() -> Unit)? = when (val view = baseComponent) {
            is DocumentQaView -> {
                { browseToBestSnippet(doc.document, view.planner.lastResult, hostServices) }
            }
            else -> null
        }
        if (doc.image == null)
            hyperlink(doc.document.shortName) {
                style = "-fx-font-size: 16px;"
                if (action != null) {
                    action(action)
                }
            }
        else
            imageview(doc.image) {
            opacity = 0.0
            timeline {
                keyframe(1.0.seconds) {
                    keyvalue(scaleXProperty(), 1.1)
                    keyvalue(scaleYProperty(), 1.1)
                }
                keyframe(2.0.seconds) {
                    keyvalue(opacityProperty(), 1.0)
                    keyvalue(scaleXProperty(), 1.0)
                    keyvalue(scaleYProperty(), 1.0)
                }
            }
            if (action != null) {
                cursor = javafx.scene.Cursor.HAND
                setOnMouseClicked { action() }
            }
        }
    }

    private fun animateThumbs(thumbs: Map<EmbeddingDocument, Image?>) {
        thumbnailList.clear()
        val entries = thumbs.entries.toList()
        val n = SimpleIntegerProperty(-1).apply {
            onChange { thumbnailList.add(DocumentThumbnail(entries[it].key, entries[it].value)) }
        }
        timeline {
            keyframe(2.0.seconds) {
                keyvalue(n, entries.size - 1)
            }
        }
    }

    private var indicatorTimeline: Timeline? = null

    private fun blinkIndicator(start: Boolean) {
        indicatorTimeline?.stop()
        if (start) {
            indicatorTimeline = timeline {
                keyframe(0.5.seconds) {
                    keyvalue(indicator.opacityProperty(), 0.1)
                }
                keyframe(1.0.seconds) {
                    keyvalue(indicator.opacityProperty(), 1.0)
                }
                cycleCount = Timeline.INDEFINITE
                setOnFinished {
                    indicator.opacity = 1.0
                }
            }
        }
    }

    //endregion
}

/** A document thumbnail object. */
private class DocumentThumbnail(val document: EmbeddingDocument, val image: Image?)

/** Animates a series of text and hyperlink objects within a [TextFlow]. */
private object TextFlowAnimator {
    fun animateText(sourceText: List<Node>, target: ObservableList<Node>, onFrame: () -> Unit, onFinished: () -> Unit) {
        // calculate length of text in this node
        fun Node.length() = (this as? Text)?.text?.length ?: (this as? Hyperlink)?.text?.length ?: 0

        // get a subset of the nodes based on the number of characters
        fun takeChars(n: Int): List<Node> {
            var taken = 0
            val result = mutableListOf<Node>()
            for (node in sourceText) {
                val length = node.length()
                if (taken + length <= n) {
                    result.add(node)
                    taken += length
                } else {
                    when (node) {
                        is Text -> result.add(
                            Text(node.text.substring(0, n - taken)).apply {
                                style = node.style
                            }
                        )
                        is Hyperlink -> result.add(
                            Hyperlink(node.text.substring(0, n - taken)).apply {
                                style = node.style
                                onAction = node.onAction
                            }
                        )
                        else -> throw UnsupportedOperationException()
                    }
                    break
                }
            }
            return result
        }

        val totalLength = sourceText.sumOf { it.length() }
        val time = minOf(5.0, 0.05 * totalLength)
        val chars = SimpleIntegerProperty(0).apply {
            onChange {
                target.setAll(takeChars(it))
                onFrame()
            }
        }
        timeline {
            keyframe(time.seconds) {
                keyvalue(chars, totalLength)
            }
            setOnFinished {
                target.setAll(sourceText)
                onFrame()
                onFinished()
            }
        }
    }
}
