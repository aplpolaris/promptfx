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
package tri.promptfx.api

import com.aallam.openai.api.chat.*
import com.aallam.openai.api.core.Role
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.image.Image
import javafx.scene.input.TransferMode
import javafx.scene.layout.Priority
import tornadofx.*
import tri.promptfx.hasImageFile
import tri.util.ui.graphic
import tri.util.ui.toUri

/** Fragment showing a history of chat messages. */
class ChatHistoryView(roles: List<Role> = listOf(Role.Assistant, Role.User)) : Fragment() {

    val components = observableListOf<ChatMessageUiModel>().apply {
        add(ChatMessageUiModel(ChatRole.User, ""))
    }

    override val root = vbox {
        vgrow = Priority.ALWAYS
        spacing = 10.0
        listview(components) {
            vgrow = Priority.ALWAYS
            cellFormat {
                graphic = ChatHistoryItem(it, roles, remove = { components.remove(it) }).root
            }
        }
        hbox {
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
        chatMessage {
            role = it.role
            name = it.name?.ifBlank { null }
            content {
                if (it.content.isNotBlank())
                    text(it.content)
                if (it.contentImage != null)
                    image(it.contentImage!!.url, it.detailImageProperty.value.let { if (it == "auto") null else it })
            }
            toolCalls = it.toolCalls
            toolCallId = it.toolCallId
        }
    }
}

/** UI for a single chat message. */
class ChatHistoryItem(chat: ChatMessageUiModel, roles: List<Role>, remove: () -> Unit) : Fragment() {
    override val root = vbox(10.0) {
        hbox {
            spacing = 10.0
            // only allow user or assistant per intended API use
            combobox(chat.roleProperty, roles) {
                cellFormat { text = it.role }
            }
            hbox(5.0, Pos.CENTER_LEFT) {
                managedWhen(chat.contentImageProperty.isNotNull)
                visibleWhen(chat.contentImageProperty.isNotNull)
                style = "-fx-background-color: #f0f0f0;"
                text("Image:")
                button(chat.detailImageProperty) {
                    action {
                        chat.detailImageProperty.value = when (chat.detailImageProperty.value) {
                            "auto" -> "low"
                            "low" -> "high"
                            else -> "auto"
                        }
                    }
                }
                button("", FontAwesomeIcon.MINUS_SQUARE.graphic) {
                    action {
                        chat.contentImageProperty.set(null)
                    }
                }
            }
            hbox {
                alignment = Pos.CENTER
                spacing = 10.0
                managedWhen(chat.roleProperty.isEqualTo(Role.Tool))
                visibleWhen(chat.roleProperty.isEqualTo(Role.Tool))
                text("id:")
                textfield(chat.toolCallIdProperty) {
                    isEditable = false
                }
            }
            textfield(chat.nameProperty) {
                visibleWhen(chat.nameProperty.isNotBlank())
                tooltip("The name of the author of this message. name is required if role is function, and it should be the name of the function whose response is in the content.")
            }
            spacer()
            button("", FontAwesomeIconView(FontAwesomeIcon.MINUS_CIRCLE)) {
                action { remove() }
            }
        }
        hbox {
            alignment = Pos.CENTER
            spacing = 10.0
            managedWhen(chat.toolCallsNameProperty.isNotBlank())
            visibleWhen(chat.toolCallsNameProperty.isNotBlank())
            text("tool:")
            textfield(chat.toolCallsNameProperty) {
                isEditable = false
            }
            text("args:")
            textfield(chat.toolCallsArgsProperty) {
                isEditable = false
                hgrow = Priority.ALWAYS
            }
            text("id:")
            textfield(chat.toolCallsIdProperty) {
                isEditable = false
            }
        }
        hbox(10.0) {
            textarea(chat.contentProperty) {
                managedWhen(chat.roleProperty.isEqualTo(ChatRole.User).or(chat.contentProperty.isNotBlank()))
                visibleWhen(chat.roleProperty.isEqualTo(ChatRole.User).or(chat.contentProperty.isNotBlank()))
                hgrow = Priority.ALWAYS
                prefRowCount = 3
                isWrapText = true
            }
            imagethumbnail(chat.contentImageProperty)
            setOnDragOver {
                if (it.dragboard.hasImage() || it.dragboard.hasImageFile()) {
                    it.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
                }
                it.consume()
            }
            setOnDragDropped { it
                if (it.dragboard.hasImage()) {
                    chat.contentImageProperty.set(ImagePart.ImageURL(it.dragboard.image.toUri()))
                } else if (it.dragboard.hasImageFile()) {
                    chat.contentImageProperty.set(ImagePart.ImageURL(Image(it.dragboard.files.first().toURI().toString()).toUri()))
                }
                it.isDropCompleted = true
                it.consume()
            }
        }
    }
}

/** Adds an image thumbnail of given size, with optional ability to edit. */
fun EventTarget.imagethumbnail(image: SimpleObjectProperty<ImagePart.ImageURL>, size: Int = 128) {
    imageview {
        managedWhen(image.isNotNull)
        visibleWhen(image.isNotNull)
        imageProperty().bind(image.objectBinding { it?.let { Image(it.url) } })
        fitWidth = size.toDouble()
        fitHeight = size.toDouble()
        isPreserveRatio = true
        isSmooth = true
    }
}

/** UI model for a chat message. */
class ChatMessageUiModel(
    role: ChatRole = ChatRole.User,
    content: String = "",
    contentImage: ImagePart.ImageURL? = null,
    name: String? = null,
    _toolCalls: List<ToolCall.Function>? = null,
    _toolCallId: ToolId? = null
) : ViewModel() {
    val roleProperty = SimpleObjectProperty(role)
    val role: ChatRole by roleProperty

    val contentProperty = SimpleStringProperty(content)
    var content: String by contentProperty

    val contentImageProperty = SimpleObjectProperty<ImagePart.ImageURL>(contentImage)
    var contentImage: ImagePart.ImageURL? by contentImageProperty
    val detailImageProperty = SimpleStringProperty("auto")

    val nameProperty = SimpleStringProperty(name)
    var name: String? by nameProperty

    val toolCalls: List<ToolCall.Function>? = _toolCalls
    val toolCallId: ToolId? = _toolCallId

    val toolCallsNameProperty = SimpleStringProperty(_toolCalls?.joinToString(" -- ") { it.function.name })
    val toolCallsArgsProperty = SimpleStringProperty(_toolCalls?.joinToString(" -- ") { it.function.arguments })
    val toolCallsIdProperty = SimpleStringProperty(_toolCalls?.joinToString(" -- ") { it.id.id })
    val toolCallIdProperty = SimpleStringProperty(_toolCallId?.id)

    companion object {
        fun valueOf(it: ChatMessage) =
            ChatMessageUiModel(
                role = it.role,
                content = it.content ?: "",
                contentImage = it.imageContent(),
                name = it.name,
                _toolCalls = it.toolCalls?.filterIsInstance<ToolCall.Function>(),
                _toolCallId = it.toolCallId
            )

        /** Find first image content in a message, if present. */
        fun ChatMessage.imageContent() =
            ((messageContent as? ListContent)?.content?.firstOrNull { it is ImagePart } as? ImagePart)?.imageUrl
    }
}
