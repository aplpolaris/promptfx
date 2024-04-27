/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.util.ui

import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.ListChangeListener
import javafx.scene.Node
import javafx.scene.control.Hyperlink
import javafx.scene.control.ScrollPane
import javafx.scene.input.DataFormat
import javafx.scene.layout.HBox
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.Screen
import tornadofx.*

/** View for formatted text that can be animated. */
class AnimatingTextFlow : Fragment() {

    val textNodes = observableListOf<Node>()

    override val root = hbox {
        scrollpane {
            id = "chat-response-scroll"
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
            maxHeight = Screen.getPrimary().bounds.height / 2
            hbox {
                textflow {
                    id = "chat-response"
                    textNodes.onChange { updateTextFlow(it) }
                    prefWidth = minOf(2000.0, Screen.getPrimary().bounds.width * 2 / 3)

                    // add context menu to copy
                    contextmenu {
                        item("Copy output to clipboard") {
                            action {
                                clipboard.setContent(mapOf(
                                    DataFormat.PLAIN_TEXT to plainText()
                                ))
                            }
                        }
                    }
                }
                vbox { prefWidth = 20.0 }
            }
        }
    }

    fun updatePrefWidth(width: Double) {
        (((root.children[0] as ScrollPane).content as HBox).children[0] as TextFlow).prefWidth = width
//        (root.lookup("chat-response") as TextFlow).prefWidth = width
    }

    fun updateFontSize(size: Double) {
        (((root.children[0] as ScrollPane).content as HBox).children[0] as TextFlow).style += "-fx-font-size: ${size}px;"
    }

    private fun TextFlow.updateTextFlow(change: ListChangeListener.Change<out Node>) {
        children.clear()
        children.addAll(change.list)
        children.filterIsInstance<Text>().forEach {
            it.styleClass += "chat-text-default"
        }
    }

    /** Animates a series of text and hyperlink objects within a [TextFlow]. */
    fun animateText(
        sourceText: List<Node>,
        onFrame: () -> Unit = { (root.scene?.lookup("#chat-response-scroll") as? ScrollPane)?.vvalue = 1.0 },
        onFinished: () -> Unit = { }
    ) {

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
                textNodes.setAll(takeChars(it))
                onFrame()
            }
        }
        timeline {
            keyframe(time.seconds) {
                keyvalue(chars, totalLength)
            }
            setOnFinished {
                textNodes.setAll(sourceText)
                onFrame()
                onFinished()
            }
        }
    }
}
