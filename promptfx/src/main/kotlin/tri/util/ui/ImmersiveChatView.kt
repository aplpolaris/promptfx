package tri.util.ui

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.animation.Timeline
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.stage.Screen
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.embedding.EmbeddingDocument
import tri.promptfx.DocumentUtils.browseToDocument
import tri.promptfx.DocumentUtils.documentThumbnail
import tri.promptfx.PromptFxController
import tri.promptfx.apps.DocumentQaView

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

/** View for a full-screen chat display. */
class ImmersiveChatView : Fragment("Immersive Chat") {

    val onUserRequest: suspend (String) -> String by param()
    val baseComponentTitle: String? by param()
    val baseComponent: View? by param()

    val indicator = FontAwesomeIcon.ROCKET.graphic.also {
        it.glyphSize = 60.0
        it.glyphStyle = "-fx-fill: white;"
    }

    val controller: PromptFxController by inject()
    val thumbnailList = observableListOf<Pair<EmbeddingDocument, Image>>()
    val input = SimpleStringProperty("")
    val response = SimpleStringProperty("")

    val css = ImmersiveChatView::class.java.getResource("resources/chat.css")!!

    init {
        if (baseComponent is DocumentQaView) {
            val base = baseComponent as DocumentQaView
            base.snippets.onChange {
                val thumbs = base.snippets.map { it.document }.toSet()
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
                    text(response) {
                        id = "chat-response"
                        wrappingWidth = minOf(2000.0, Screen.getPrimary().bounds.width * 2 / 3)
                    }
                    vbox { prefWidth = 20.0 }
                }
            }
        }

        hbox {
            alignment = Pos.CENTER
            prefHeight = 0.25 * screenHeight
            spacing = 40.0
            children.bind(thumbnailList) {
                docthumbnail(it.first, it.second)
            }
        }
    }

    private fun handleUserAction() {
        response.set("")
        blinkIndicator(start = true)
        runAsync {
            runBlocking {
                onUserRequest(input.value)
            }
        } ui {
            blinkIndicator(start = false)
            controller.updateUsage()
            response.animateText(it)
        }
    }

    //region ANIMATION

    private fun EventTarget.docthumbnail(document: EmbeddingDocument, image: Image) = vbox {
        imageview(image) {
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
            cursor = javafx.scene.Cursor.HAND
            setOnMouseClicked {
                browseToDocument(document)
            }
        }
    }

    private fun SimpleStringProperty.animateText(text: String) {
        val chars = SimpleIntegerProperty(0).apply {
            onChange {
                set(text.take(it))
                (root.scene.lookup("#chat-response-scroll") as ScrollPane).vvalue = 1.0
            }
        }
        val time = minOf(5.0, 0.05 * text.length)
        timeline {
            keyframe(time.seconds) {
                keyvalue(chars, text.length)
            }
            setOnFinished {
                (root.scene.lookup("#chat-input") as TextField).selectAll()
            }
        }
    }

    private fun animateThumbs(thumbs: Map<EmbeddingDocument, Image?>) {
        thumbnailList.clear()
        val entries = thumbs.entries.filter { it.value != null }.map { it.key to it.value!! }
        val n = SimpleIntegerProperty(-1).apply {
            onChange { thumbnailList.add(entries[it]) }
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
