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
package tri.promptfx

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.layout.Priority
import tornadofx.*

@OptIn(BetaOpenAI::class)
class ChatHistoryView : Fragment() {

    val components = observableListOf<ChatLineModel>().apply {
        add(ChatLineModel(ChatRole.User, ""))
    }

    override val root = vbox {
        spacing = 10.0
        listview(components) {
            vgrow = Priority.ALWAYS
            cellFormat {
                graphic = hbox {
                    spacing = 10.0
                    combobox(it.roleProperty, listOf(ChatRole.User, ChatRole.Assistant, ChatRole.System)) {
                        cellFormat { text = it.role }
                    }
                    textfield(it.textProperty) {
                        hgrow = Priority.ALWAYS
                    }
                    button("", FontAwesomeIconView(FontAwesomeIcon.MINUS_CIRCLE)) {
                        action { components.remove(it) }
                    }
                }
            }
        }
        hbox {
            spacing = 10.0
            button("Add message", FontAwesomeIconView(FontAwesomeIcon.PLUS_CIRCLE)) {
                action {
                    components.add(ChatLineModel())
                }
            }
        }
    }

    fun chatMessages() = components.map {
        ChatMessage(it.role, it.text)
    }

}

@OptIn(BetaOpenAI::class)
class ChatLineModel(role: ChatRole = ChatRole.User, text: String = "") : ViewModel() {
    val roleProperty = SimpleObjectProperty(role)
    val role by roleProperty

    val textProperty = SimpleStringProperty(text)
    var text by textProperty
}
