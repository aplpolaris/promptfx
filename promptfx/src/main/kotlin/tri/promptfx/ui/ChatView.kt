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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.animation.Timeline
import javafx.scene.control.TextField
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import kotlinx.coroutines.runBlocking
import tornadofx.*

/** A view with a chat panel and an entry text box. */
class ChatView: Fragment() {

    private val viewScope = Scope()
    private val panel = find<ChatPanel>(viewScope)
    private val chats = panel.chats
    private val chatDriver: ChatDriver by inject()

    private lateinit var chatField: TextField
    private lateinit var indicator: FontAwesomeIconView

    private val chatHistorySize = 1

    override val root = borderpane {
        style {
            backgroundColor += Color.WHITE
            spacing = 10.px
        }

        // toolbar
        top = toolbar {
            spacer()
            indicator = FontAwesomeIconView(FontAwesomeIcon.ROCKET).apply {
                glyphSize = 18.0
                opacity = 0.05
            }
            indicator.attachTo(this)
        }

        // history panel
        center = panel.root

        // input panel
        bottom = hbox {
            padding = insets(10)
            spacing = 5.0
            alignment = javafx.geometry.Pos.CENTER
            chatField = textfield {
                prefWidth = 400.0
                maxWidth = Double.MAX_VALUE
                vgrow = Priority.ALWAYS
                action { userChat(text) }
            }
            button("", FontAwesomeIconView(FontAwesomeIcon.SEND)) {
                action { userChat(chatField.text) }
            }
            button("", FontAwesomeIconView(FontAwesomeIcon.REFRESH)) {
                action { retryChat() }
            }
            button("", FontAwesomeIconView(FontAwesomeIcon.TRASH)) {
                action { clearChats() }
            }
        }
    }

    private fun userChat(text: String) {
        val message = text.trim()
        if (message.isNotEmpty()) {
            chats.add(ChatEntry("", message, ChatRoleStyle.USER))
            generateChatResponse()
            chatField.clear()
        }
    }

    private fun generateChatResponse() {
        blinkIndicator(start = true)
        runAsync {
            runBlocking {
                chatDriver.chat(chats.takeLast(chatHistorySize))
            }
        } ui {
            chats.add(it)
            blinkIndicator(start = false)
        }
    }

    private fun retryChat() {
        chats.remove(chats.last())
        generateChatResponse()
    }

    private fun clearChats() {
        chats.clear()
    }

    //region ANIMATION WHILE THINKING

    private var indicatorBlinkTimeline: Timeline? = null

    private fun blinkIndicator(start: Boolean) {
        indicatorBlinkTimeline?.stop()
        if (start) {
            indicatorBlinkTimeline = timeline {
                keyframe(0.5.seconds) { keyvalue(indicator.opacityProperty(), 1.0) }
                keyframe(1.0.seconds) { keyvalue(indicator.opacityProperty(), 0.1) }
                cycleCount = Timeline.INDEFINITE
                setOnFinished { indicator.opacity = 0.06 }
            }
        }
    }

    //endregion

}
