/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.binding.BooleanBinding
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.image.Image
import javafx.scene.input.TransferMode
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.core.*
import tri.promptfx.hasImageFile
import tri.util.ui.graphic
import tri.util.ui.imageUri
import java.io.FileInputStream
import java.net.URI
import java.net.URL

/** Fragment showing a history of chat messages. */
class ChatHistoryView(roles: List<MChatRole> = listOf(MChatRole.Assistant, MChatRole.User)) : Fragment() {

    val components = observableListOf<ChatMessageUiModel>().apply {
        add(ChatMessageUiModel(MChatRole.User, ""))
    }

    override val root = vbox {
        vgrow = Priority.ALWAYS
        spacing = 10.0
        listview(components) {
            vgrow = Priority.ALWAYS
            isFillWidth = true
            selectionModel = null
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
        chatMessage(it.role) {
//            name = it.name?.ifBlank { null }
            if (it.contentText.isNotBlank())
                text(it.contentText)
            if (it.contentImage != null)
                image(it.contentImage.toString())
                // TODO - add detailImageProperty to API (used in OpenAI)
                // it.detailImageProperty.value.let { if (it == "auto") null else it })
            toolCalls(it.toolCalls ?: listOf())
            toolCallId(it.toolCallId)
        }
    }
}

/** UI for a single chat message. */
class ChatHistoryItem(val chat: ChatMessageUiModel, roles: List<MChatRole>, remove: () -> Unit) : Fragment() {
    override val root = vbox(10.0) {
        hbox(5.0) {
            // only allow user or assistant per intended API use
            combobox(chat.roleProperty, roles)
//                cellFormat { text = it.toString() }
            hbox(5.0, Pos.CENTER_LEFT) {
                managedWhen(chat.contentImageProperty.isNotNull)
                visibleWhen(chat.contentImageProperty.isNotNull)
                padding = insets(5.0, 2.0)
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
            hbox(5.0, Pos.CENTER_LEFT) {
                managedWhen(chat.roleProperty.isEqualTo(MChatRole.Tool))
                visibleWhen(chat.roleProperty.isEqualTo(MChatRole.Tool))
                text("id/name:")
                textfield(chat.toolCallIdProperty) {
                    isEditable = false
                }
            }
            textfield(chat.nameProperty) {
                visibleWhen(chat.nameProperty.isNotBlank())
                tooltip("The name of the author of this message. name is required if role is function, and it should be the name of the function whose response is in the content.")
            }
            spacer()
            if (chat.messageList.sizeProperty.greaterThan(1).value) {
                button("", FontAwesomeIcon.ANGLE_DOUBLE_LEFT.graphic) {
                    enableWhen(chat.messageListIndex.greaterThan(0))
                    action { previousChat() }
                }
                button("", FontAwesomeIcon.ANGLE_DOUBLE_RIGHT.graphic) {
                    enableWhen(chat.messageListIndex.lessThan(chat.messageList.sizeProperty.subtract(1)))
                    action { nextChat() }
                }
            }
            button("", FontAwesomeIconView(FontAwesomeIcon.MINUS_CIRCLE)) {
                action { remove() }
            }
        }
        hbox {
            alignment = Pos.CENTER
            spacing = 10.0
            managedWhen(chat.toolCallsNameProperty.isNotBlank().and(chat.roleProperty.isEqualTo(MChatRole.Assistant)))
            visibleWhen(chat.toolCallsNameProperty.isNotBlank().and(chat.roleProperty.isEqualTo(MChatRole.Assistant)))
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
        hbox(5.0) {
            textarea(chat.contentTextProperty) {
                managedWhen(chat.roleProperty.isEqualTo(MChatRole.User).or(chat.contentTextProperty.isNotBlank()))
                visibleWhen(chat.roleProperty.isEqualTo(MChatRole.User).or(chat.contentTextProperty.isNotBlank()))
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
                    chat.contentImageProperty.set(URI.create(it.dragboard.image.imageUri()))
                } else if (it.dragboard.hasImageFile()) {
                    chat.contentImageProperty.set(URI.create(Image(FileInputStream(it.dragboard.files.first())).imageUri()))
                }
                it.isDropCompleted = true
                it.consume()
            }
        }
    }

    private fun previousChat() {
        chat.messageListIndex.minusAssign(1)
        chat.copyFrom(chat.messageList[chat.messageListIndex.value])
    }

    private fun nextChat() {
        chat.messageListIndex.plusAssign(1)
        chat.copyFrom(chat.messageList[chat.messageListIndex.value])
    }
}

/** Adds an image thumbnail of given size, with optional ability to edit. */
fun EventTarget.imagethumbnail(image: ObservableValue<URI>, size: Int = 128) {
    imageview {
        managedWhen(BooleanBinding.booleanExpression(image.map { it != null }))
        visibleWhen(BooleanBinding.booleanExpression(image.map { it != null }))
        imageProperty().bind(image.objectBinding { it?.let { Image(it.toString()) } })
        fitWidth = size.toDouble()
        fitHeight = size.toDouble()
        isPreserveRatio = true
        isSmooth = true
    }
}

/** UI model for a chat message. */
class ChatMessageUiModel(
    role: MChatRole = MChatRole.User,
    contentText: String = "",
    contentImage: URI? = null,
    name: String? = null,
    _toolCalls: List<MToolCall>? = null,
    _toolCallId: String? = null
) : ViewModel() {
    val roleProperty = SimpleObjectProperty(role)
    val role: MChatRole by roleProperty

    val contentTextProperty = SimpleStringProperty(contentText)
    val contentText: String by this.contentTextProperty

    val contentImageProperty = SimpleObjectProperty<URI>(contentImage)
    var contentImage: URI? by contentImageProperty
    val detailImageProperty = SimpleStringProperty("auto")

    val nameProperty = SimpleStringProperty(name)
    var name: String? by nameProperty

    val toolCalls: List<MToolCall>? = _toolCalls
    val toolCallId: String? = _toolCallId

    val toolCallsNameProperty = SimpleStringProperty(_toolCalls?.joinToString(" -- ") { it.name })
    val toolCallsArgsProperty = SimpleStringProperty(_toolCalls?.joinToString(" -- ") { it.argumentsAsJson })
    val toolCallsIdProperty = SimpleStringProperty(_toolCalls?.joinToString(" -- ") { it.id })
    val toolCallIdProperty = SimpleStringProperty(_toolCallId)

    var messageList = observableListOf<ChatMessageUiModel>()
    val messageListEmpty = messageList.sizeProperty.isEqualTo(0)
    var messageListIndex = SimpleIntegerProperty(-1)

    /** Copy parameters from second model. */
    fun copyFrom(other: ChatMessageUiModel) {
        roleProperty.set(other.role)
        contentTextProperty.set(other.contentText)
        contentImageProperty.set(other.contentImage)
        nameProperty.set(other.name)
        toolCallsNameProperty.set(other.toolCallsNameProperty.value)
        toolCallsArgsProperty.set(other.toolCallsArgsProperty.value)
        toolCallsIdProperty.set(other.toolCallsIdProperty.value)
        toolCallIdProperty.set(other.toolCallIdProperty.value)
    }

    companion object {
        /** Create UI model from chat message. */
        fun valueOf(it: MultimodalChatMessage) =
            ChatMessageUiModel(
                name = it.toolCallId,
                role = it.role,
                contentText = it.content?.firstOrNull()?.text ?: "",
                contentImage = it.imageUri(),
                _toolCalls = it.toolCalls,
                _toolCallId = it.toolCallId
            )

        /** Create UI model with multiple chat message options. */
        fun valueOf(chats: List<MultimodalChatMessage>) = valueOf(chats.first()).apply {
            messageList.setAll(chats.map { valueOf(it) })
            messageListIndex.set(0)
        }

        /** Find first image content in a message, if present. */
        fun MultimodalChatMessage.imageUri(): URI? =
            content?.firstOrNull { it.partType == MPartType.IMAGE }?.let { URI.create(it.inlineData) }
    }
}
