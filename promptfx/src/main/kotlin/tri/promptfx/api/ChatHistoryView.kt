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
package tri.promptfx.api

import com.aallam.openai.api.chat.*
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*

class ChatHistoryView : Fragment() {

    val components = observableListOf<ChatMessageUiModel>().apply {
        add(ChatMessageUiModel(ChatRole.User, ""))
    }

    override val root = vbox {
        vgrow = Priority.ALWAYS
        spacing = 10.0
        listview(components) {
            vgrow = Priority.ALWAYS
            cellFormat {
                graphic = vbox {
                    spacing = 10.0
                    hbox {
                        spacing = 10.0
                        // only allow user or assistant per intended API use
                        combobox(it.roleProperty, listOf(ChatRole.User, ChatRole.Assistant)) {
                            cellFormat { text = it.role }
                        }
                        textfield(it.nameProperty) {
                            visibleWhen(it.nameProperty.isNotBlank())
                            tooltip("The name of the author of this message. name is required if role is function, and it should be the name of the function whose response is in the content.")
                            hgrow = Priority.ALWAYS
                        }
                        button("", FontAwesomeIconView(FontAwesomeIcon.MINUS_CIRCLE)) {
                            action { components.remove(it) }
                        }
                    }
                    hbox {
                        alignment = Pos.CENTER
                        spacing = 10.0
                        managedWhen(it.toolCallNameProperty.isNotBlank())
                        visibleWhen(it.toolCallNameProperty.isNotBlank())
                        text("tool:")
                        textfield(it.toolCallNameProperty) {
                            isEditable = false
                        }
                        text("args:")
                        textfield(it.toolCallArgsProperty) {
                            isEditable = false
                            hgrow = Priority.ALWAYS
                        }
                    }
                    textarea(it.contentProperty) {
                        managedWhen(it.roleProperty.isEqualTo(ChatRole.User).or(it.contentProperty.isNotBlank()))
                        visibleWhen(it.roleProperty.isEqualTo(ChatRole.User).or(it.contentProperty.isNotBlank()))
                        hgrow = Priority.ALWAYS
                        prefRowCount = 3
                        isWrapText = true
                    }
                }
            }
        }
        toolbar {
            spacing = 10.0
            button("Add message", FontAwesomeIconView(FontAwesomeIcon.PLUS_CIRCLE)) {
                action { components.add(ChatMessageUiModel()) }
            }
            button("Clear", FontAwesomeIconView(FontAwesomeIcon.TRASH)) {
                action { components.clear() }
            }
        }
    }

    fun chatMessages() = components.map {
        ChatMessage(it.role, it.content, it.name?.ifBlank { null }, toolCalls = it.toolCalls)
    }

}

class ChatMessageUiModel(
    role: ChatRole = ChatRole.User,
    content: String = "",
    name: String? = null,
    _toolCalls: List<ToolCall.Function>? = null
) : ViewModel() {
    val roleProperty = SimpleObjectProperty(role)
    val role: ChatRole by roleProperty

    val contentProperty = SimpleStringProperty(content)
    var content: String by contentProperty

    val nameProperty = SimpleStringProperty(name)
    var name: String? by nameProperty

    val toolCalls: List<ToolCall.Function>? = _toolCalls

    val toolCallNameProperty = SimpleStringProperty(_toolCalls?.joinToString(", ") { it.function.name })
    var toolCallName: String? by toolCallNameProperty

    val toolCallArgsProperty = SimpleStringProperty(_toolCalls?.joinToString("\n") { it.function.arguments })
    var toolCallArgs: String by toolCallArgsProperty

    companion object {
        fun valueOf(it: ChatMessage) =
            ChatMessageUiModel(
                role = it.role,
                content = it.content ?: "",
                name = it.name,
                _toolCalls = it.toolCalls?.filterIsInstance<ToolCall.Function>()
            )
    }
}
