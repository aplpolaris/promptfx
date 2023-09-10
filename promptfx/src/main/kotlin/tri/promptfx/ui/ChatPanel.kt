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
package tri.promptfx.ui

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.event.EventTarget
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*
import kotlin.math.abs

/** A generic panel that shows labeled text within a scrolling vertical pane. */
class ChatPanel: Fragment() {

    val chats = observableListOf<ChatEntry>()
    var randomColors: Boolean = true

    override val root = scrollpane(fitToWidth = true) {
        maxWidth = 500.0
        vgrow = Priority.ALWAYS
        hgrow = Priority.ALWAYS
        vbox {
            padding = insets(10)
            spacing = 5.0
            bindChildren(chats) { chatmessageui(it) }
        }
    }

    private fun EventTarget.chatmessageui(chat: ChatEntry) = borderpane {
        center = vbox {
            spacing = 5.0
            hbox {
                if (chat.style.rightAlign) {
                    spacer(Priority.ALWAYS)
                }
                label(chat.user, FontAwesomeIconView(chat.style.glyph).apply {
                    glyphStyle = glyphStyle(chat)
                    glyphSize = 24.0
                }).apply {
                    style {
                        fontWeight = FontWeight.BOLD
                        textFill = textFill(chat)
                    }
                }
            }
            hbox {
                if (chat.style.rightAlign) {
                    spacer(Priority.ALWAYS)
                }
                label(chat.message).apply {
                    isWrapText = true
                    style {
                        backgroundRadius += box(11.px)
                        borderRadius += box(10.px)
                        borderColor += box(Color.WHITE)
                        borderWidth += box(1.px)
                        padding = box(5.px)
                        updateColors(chat)
                    }
                }
            }
        }
    }

    // cache of colors by user, used when randomizing colors
    private val userColors = mutableMapOf<String, Color>()
    // hard-coded, first four hues to use
    private val hues = listOf(210.0, 120.0, 270.0)

    private fun InlineCss.updateColors(chat: ChatEntry) {
        if (randomColors) {
            val color = randomColor(chat.user)
            backgroundColor += color
            textFill = Color.hsb(color.hue, color.saturation, color.brightness * 0.2)
        } else {
            backgroundColor += chat.style.background
            textFill = chat.style.text
        }
    }

    private fun textFill(chat: ChatEntry) = if (randomColors) {
        val color = randomColor(chat.user)
        Color.hsb(color.hue, color.saturation, color.brightness * 0.5)
    } else {
        chat.style.text
    }

    private fun glyphStyle(chat: ChatEntry) = if (randomColors) {
        randomColor(chat.user).let {
            "-fx-fill: ${Color.hsb(it.hue, it.saturation, it.brightness * 0.5).css};"
        }
    } else {
        chat.style.glyphStyle
    }

    private fun randomColor(user: String) = userColors.getOrPut(user) {
        if (userColors.size < hues.size) {
            Color.hsb(hues[userColors.size], 0.5, 0.9)
        } else {
            val closeness = 360.0/userColors.size/2
            generateSequence { Color.hsb(Math.random() * 360, 0.5, 0.9) }
                .first { color -> userColors.none { abs(it.value.hue - color.hue) < closeness } }
        }
    }

}

