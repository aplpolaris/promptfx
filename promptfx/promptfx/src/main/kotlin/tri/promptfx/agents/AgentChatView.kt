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
package tri.promptfx.agents

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.*
import javafx.geometry.Pos
import javafx.scene.control.ListView
import javafx.scene.image.Image
import javafx.scene.input.TransferMode
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.stage.FileChooser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import tornadofx.*
import tri.ai.core.*
import tri.ai.core.agent.AgentChatConfig
import tri.ai.core.agent.AgentChatSession
import tri.ai.core.agent.DefaultAgentChat
import tri.ai.core.agent.api.DefaultAgentChatSessionManager
import tri.ai.pips.ExecEvent
import tri.promptfx.PromptFxModels
import tri.promptfx.hasImageFile
import tri.util.ui.BlinkingIndicator
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.imageUri
import java.net.URI

/** Plugin for the [AgentChatView]. */
class AgentChatPlugin : NavigableWorkspaceViewImpl<AgentChatView>("Agents", "Agent Chat", type = AgentChatView::class)

/** A chat view backed by [AgentChatSession], with a session sidebar, message history, image upload, and streaming. */
class AgentChatView : View("Agent Chat") {

    //region DEPENDENCIES

    private val sessionManager = DefaultAgentChatSessionManager()
    private val agentChat = DefaultAgentChat()

    //endregion

    //region STATE

    private val sessions = observableListOf<AgentChatSession>()
    private val currentSession = SimpleObjectProperty<AgentChatSession?>(null)
    private val chatMessages = observableListOf<AgentMessageEntry>()
    private val inputText = SimpleStringProperty("")
    private val inputImage = SimpleObjectProperty<URI?>(null)
    private val isThinking = SimpleBooleanProperty(false)

    private var thinkingEntry: AgentMessageEntry? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var sessionListView: ListView<AgentChatSession>

    //endregion

    override val root = borderpane {
        // LEFT: session sidebar
        left = vbox(6.0) {
            padding = insets(10.0)
            prefWidth = 220.0
            style { backgroundColor += Color.web("#f0f0f0") }

            button("+ New Chat", FontAwesomeIconView(FontAwesomeIcon.PLUS)) {
                maxWidth = Double.MAX_VALUE
                action { createNewSession() }
            }

            separator()

            sessionListView = listview(sessions) {
                vgrow = Priority.ALWAYS
                prefHeight = 0.0
                cellFormat { session ->
                    graphic = borderpane {
                        center = vbox(2.0) {
                            label(session.name) {
                                isWrapText = true
                                maxWidth = 155.0
                                style { fontWeight = FontWeight.BOLD }
                            }
                            label("${session.messages.size} messages") {
                                style { textFill = Color.GRAY; fontSize = 10.px }
                            }
                        }
                        right = button("", FontAwesomeIconView(FontAwesomeIcon.TRASH)) {
                            tooltip("Delete this session")
                            action { deleteSession(session) }
                        }
                    }
                }
                onUserSelect { session ->
                    if (session != currentSession.value) {
                        switchToSession(session)
                    }
                }
            }
        }

        // CENTER: main chat area
        center = borderpane {
            // HEADER
            top = hbox(10.0, Pos.CENTER_LEFT) {
                padding = insets(8.0, 12.0)
                style { backgroundColor += Color.web("#e8e8e8") }
                label {
                    textProperty().bind(currentSession.stringBinding { it?.name ?: "No Session" })
                    style { fontWeight = FontWeight.BOLD; fontSize = 16.px }
                }
                spacer()
                val blinkIndicator = BlinkingIndicator(FontAwesomeIcon.CIRCLE).apply {
                    glyphSize = 10.0
                    glyphStyle = "-fx-fill: #888;"
                    opacity = 0.0
                }
                blinkIndicator.attachTo(this)
                isThinking.onChange {
                    if (it) blinkIndicator.startBlinking() else blinkIndicator.stopBlinking()
                }
                button("", FontAwesomeIconView(FontAwesomeIcon.COG)) {
                    tooltip("Chat Settings")
                    action { openSettingsDialog() }
                }
            }

            // MESSAGES
            center = scrollpane(fitToWidth = true) {
                vgrow = Priority.ALWAYS
                vbox(8.0) {
                    padding = insets(10.0)
                    bindChildren(chatMessages) { entry -> buildMessageBubble(entry) }
                    // Auto-scroll to bottom when new messages arrive
                    heightProperty().onChange { vvalue = 1.0 }
                }
            }

            // INPUT
            bottom = buildInputArea()
        }
    }

    //region UI BUILDERS

    private fun buildMessageBubble(entry: AgentMessageEntry) = when (entry.type) {
        MessageType.USER -> hbox(8.0) {
            spacer()
            vbox(3.0) {
                alignment = Pos.CENTER_RIGHT
                label(entry.author) {
                    style { fontWeight = FontWeight.BOLD; textFill = Color.GRAY; fontSize = 11.px }
                    alignment = Pos.CENTER_RIGHT
                }
                if (entry.imageUri != null) {
                    imageview(Image(entry.imageUri.toString())) {
                        fitWidth = 200.0
                        isPreserveRatio = true
                    }
                }
                label {
                    textProperty().bind(entry.textProperty)
                    isWrapText = true
                    maxWidth = 480.0
                    padding = insets(8.0, 12.0)
                    style {
                        backgroundColor += Color.web("#d4e6ff")
                        backgroundRadius += box(10.px)
                        fontSize = 13.px
                    }
                }
            }
        }
        MessageType.ASSISTANT -> hbox(8.0) {
            vbox(3.0) {
                alignment = Pos.CENTER_LEFT
                label(entry.author) {
                    style { fontWeight = FontWeight.BOLD; textFill = Color.web("#2E7D32"); fontSize = 11.px }
                }
                label {
                    textProperty().bind(entry.textProperty)
                    isWrapText = true
                    maxWidth = 480.0
                    padding = insets(8.0, 12.0)
                    style {
                        backgroundColor += Color.web("#e8f5e9")
                        backgroundRadius += box(10.px)
                        fontSize = 13.px
                    }
                }
            }
            spacer()
        }
        MessageType.THINKING -> hbox(8.0) {
            vbox(3.0) {
                alignment = Pos.CENTER_LEFT
                label {
                    textProperty().bind(entry.textProperty)
                    isWrapText = true
                    maxWidth = 480.0
                    padding = insets(6.0, 10.0)
                    style {
                        textFill = Color.GRAY
                        fontSize = 12.px
                        fontStyle = FontPosture.ITALIC
                    }
                }
            }
            spacer()
        }
        MessageType.INTERMEDIATE -> hbox(8.0) {
            vbox(3.0) {
                label(entry.author) {
                    style { fontWeight = FontWeight.BOLD; textFill = Color.web("#7B5EA7"); fontSize = 10.px }
                }
                label {
                    textProperty().bind(entry.textProperty)
                    isWrapText = true
                    maxWidth = 480.0
                    padding = insets(4.0, 8.0)
                    style {
                        textFill = Color.web("#555555")
                        backgroundColor += Color.web("#f5f0ff")
                        backgroundRadius += box(6.px)
                        fontSize = 11.px
                        fontStyle = FontPosture.ITALIC
                    }
                }
            }
            spacer()
        }
        MessageType.ERROR -> hbox(8.0) {
            vbox(3.0) {
                label("Error") {
                    style { fontWeight = FontWeight.BOLD; textFill = Color.RED; fontSize = 11.px }
                }
                label {
                    textProperty().bind(entry.textProperty)
                    isWrapText = true
                    maxWidth = 480.0
                    padding = insets(6.0, 10.0)
                    style {
                        textFill = Color.DARKRED
                        backgroundColor += Color.web("#fff0f0")
                        backgroundRadius += box(6.px)
                        fontSize = 12.px
                    }
                }
            }
            spacer()
        }
    }

    private fun buildInputArea() = vbox(6.0) {
        padding = insets(8.0)
        style { backgroundColor += Color.web("#f5f5f5") }

        // Image preview row (only visible when an image is attached)
        hbox(8.0, Pos.CENTER_LEFT) {
            managedWhen(inputImage.isNotNull)
            visibleWhen(inputImage.isNotNull)
            label("Image:") { style { fontWeight = FontWeight.BOLD } }
            imageview {
                managedWhen(inputImage.isNotNull)
                visibleWhen(inputImage.isNotNull)
                imageProperty().bind(inputImage.objectBinding { it?.let { Image(it.toString()) } })
                fitHeight = 64.0
                isPreserveRatio = true
            }
            button("", FontAwesomeIconView(FontAwesomeIcon.TIMES_CIRCLE)) {
                tooltip("Remove image")
                action { inputImage.set(null) }
            }
        }

        // Message input row
        hbox(6.0, Pos.CENTER) {
            // Attach image button
            button("", FontAwesomeIconView(FontAwesomeIcon.PAPERCLIP)) {
                tooltip("Attach image (or drag-and-drop an image onto the text area)")
                action { attachImage() }
            }

            // Message text area
            textarea(inputText) {
                promptText = "Type a message... (Enter to send, Shift+Enter for newline)"
                prefRowCount = 2
                isWrapText = true
                hgrow = Priority.ALWAYS

                setOnKeyPressed { event ->
                    if (event.code == javafx.scene.input.KeyCode.ENTER && !event.isShiftDown) {
                        event.consume()
                        sendMessage()
                    }
                }

                // Drag-and-drop image support
                setOnDragOver { event ->
                    if (event.dragboard.hasImage() || event.dragboard.hasImageFile()) {
                        event.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
                    }
                    event.consume()
                }
                setOnDragDropped { event ->
                    if (event.dragboard.hasImage()) {
                        inputImage.set(URI.create(event.dragboard.image.imageUri()))
                    } else if (event.dragboard.hasImageFile()) {
                        inputImage.set(event.dragboard.files.first().toURI())
                    }
                    event.isDropCompleted = true
                    event.consume()
                }
            }

            // Send button
            button("", FontAwesomeIconView(FontAwesomeIcon.PAPER_PLANE)) {
                disableWhen(inputText.isBlank().and(inputImage.isNull).or(isThinking))
                tooltip("Send message (or press Enter in the text area)")
                action { sendMessage() }
            }
        }
    }

    //endregion

    //region SESSION MANAGEMENT

    private fun createNewSession() {
        val session = AgentChatSession()
        sessionManager.saveSession(session)
        sessions.add(0, session)
        switchToSession(session)
        sessionListView.selectionModel.select(session)
    }

    private fun switchToSession(session: AgentChatSession) {
        currentSession.set(session)
        chatMessages.clear()
        // Reload messages from session history
        for (msg in session.messages) {
            val text = msg.textContent() ?: ""
            val type = when (msg.role) {
                MChatRole.User -> MessageType.USER
                MChatRole.Assistant -> MessageType.ASSISTANT
                MChatRole.System -> MessageType.INTERMEDIATE
                else -> MessageType.INTERMEDIATE
            }
            val author = msg.role.name.lowercase().replaceFirstChar { it.uppercase() }
            val imageUri = msg.content?.firstOrNull { it.partType == MPartType.IMAGE }?.inlineData
                ?.let { runCatching { URI.create(it) }.getOrNull() }
            chatMessages.add(AgentMessageEntry(type, author, text, imageUri))
        }
        thinkingEntry = null
        isThinking.set(false)
    }

    private fun deleteSession(session: AgentChatSession) {
        sessions.remove(session)
        sessionManager.deleteSession(session.sessionId)
        if (currentSession.value == session) {
            if (sessions.isNotEmpty()) {
                switchToSession(sessions.first())
                sessionListView.selectionModel.select(sessions.first())
            } else {
                currentSession.set(null)
                chatMessages.clear()
                thinkingEntry = null
                isThinking.set(false)
            }
        }
    }

    //endregion

    //region MESSAGE SENDING

    private fun sendMessage() {
        val text = inputText.value?.trim() ?: ""
        val img = inputImage.value
        if (text.isBlank() && img == null) return

        val session = currentSession.value ?: return
        inputText.set("")
        inputImage.set(null)

        // Convert file: URIs to base64 data URIs — the API cannot download local files
        val imageData = img?.toApiImageString()

        // Build the multimodal message
        val message = if (imageData != null) {
            chatMessage(MChatRole.User) {
                if (text.isNotBlank()) text(text)
                image(imageData)
            }
        } else {
            MultimodalChatMessage.user(text)
        }

        // Show user message in UI
        chatMessages.add(AgentMessageEntry(MessageType.USER, "You", text, img))
        startThinking()

        // Send via agent chat in background
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val flow = agentChat.sendMessage(session, message)
                flow.events.collect { event ->
                    runLater { handleEvent(event, session) }
                }
            } catch (e: Exception) {
                runLater {
                    stopThinking()
                    chatMessages.add(AgentMessageEntry(MessageType.ERROR, "Error", e.message ?: "Unknown error"))
                }
            }
        }
    }

    private fun handleEvent(event: ExecEvent, session: AgentChatSession) {
        when (event) {
            is ExecEvent.Progress -> {
                updateThinkingText(event.message)
            }
            is ExecEvent.Reasoning -> {
                removeThinkingEntry()
                chatMessages.add(AgentMessageEntry(MessageType.INTERMEDIATE, "Reasoning", event.reasoning))
                startThinking()
            }
            is ExecEvent.PlanningTask -> {
                removeThinkingEntry()
                chatMessages.add(AgentMessageEntry(MessageType.INTERMEDIATE, "Planning", "${event.taskId}: ${event.description}"))
                startThinking()
            }
            is ExecEvent.UsingTool -> {
                removeThinkingEntry()
                chatMessages.add(AgentMessageEntry(MessageType.INTERMEDIATE, "Tool", "${event.toolName}: ${event.input}"))
                startThinking()
            }
            is ExecEvent.ToolResult -> {
                removeThinkingEntry()
                chatMessages.add(AgentMessageEntry(MessageType.INTERMEDIATE, "Tool Result", "${event.toolName}: ${event.result}"))
                startThinking()
            }
            is ExecEvent.StreamingToken -> {
                // Accumulate streaming tokens into the thinking entry text
                val entry = thinkingEntry
                if (entry != null && entry.type == MessageType.THINKING) {
                    val current = entry.text
                    if (current == "• • •" || current.isEmpty()) {
                        entry.text = event.token
                    } else {
                        entry.text = current + event.token
                    }
                }
            }
            is ExecEvent.Response -> {
                stopThinking()
                val responseText = event.response.message.textContent() ?: ""
                chatMessages.add(AgentMessageEntry(MessageType.ASSISTANT, "Assistant", responseText))
                // Refresh session in sidebar to show updated message count
                refreshCurrentSessionInSidebar(session)
            }
            is ExecEvent.Error -> {
                stopThinking()
                chatMessages.add(AgentMessageEntry(MessageType.ERROR, "Error", event.error.message ?: "Unknown error"))
            }
            else -> { /* ignore task lifecycle events */ }
        }
    }

    private fun refreshCurrentSessionInSidebar(session: AgentChatSession) {
        val idx = sessions.indexOf(session)
        if (idx >= 0) {
            // Trigger a list refresh by replacing with the same object
            sessions[idx] = session
            sessionListView.selectionModel.select(session)
        }
    }

    //endregion

    //region THINKING INDICATOR

    private fun startThinking() {
        isThinking.set(true)
        if (thinkingEntry == null) {
            val entry = AgentMessageEntry(MessageType.THINKING, "Assistant", "• • •")
            thinkingEntry = entry
            chatMessages.add(entry)
        }
    }

    private fun updateThinkingText(text: String) {
        thinkingEntry?.text = text
    }

    private fun removeThinkingEntry() {
        thinkingEntry?.let { chatMessages.remove(it) }
        thinkingEntry = null
    }

    private fun stopThinking() {
        isThinking.set(false)
        removeThinkingEntry()
    }

    //endregion

    //region IMAGE ATTACHMENT

    private fun attachImage() {
        val fileChooser = FileChooser().apply {
            title = "Select Image"
            extensionFilters.addAll(
                FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp")
            )
        }
        val file = fileChooser.showOpenDialog(currentStage) ?: return
        inputImage.set(file.toURI())
    }

    //endregion

    //region SETTINGS DIALOG

    private fun openSettingsDialog() {
        val session = currentSession.value ?: return
        find<AgentChatSettingsDialog>(
            AgentChatSettingsDialog::session to session
        ).openModal(block = true)
    }

    //endregion

    override fun onDock() {
        // Create an initial session the first time the view is shown (root is now initialized)
        if (sessions.isEmpty()) {
            createNewSession()
        }
    }

    override fun onUndock() {
        coroutineScope.cancel()
    }
}

/**
 * Converts a URI to a string suitable for the chat API image field.
 * Local [file:] URIs are read from disk and returned as a base64 `data:` URI so that remote APIs
 * (e.g. OpenAI) can process them; `data:` and HTTP(S) URIs are returned unchanged.
 */
private fun URI.toApiImageString(): String {
    if (scheme == "file") {
        val file = java.io.File(this)
        val ext = file.extension.lowercase()
        val mimeType = when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> "image/png"
        }
        val base64 = java.util.Base64.getEncoder().encodeToString(file.readBytes())
        return "data:$mimeType;base64,$base64"
    }
    return toString()
}

/** Dialog for configuring [AgentChatSession] settings. */
class AgentChatSettingsDialog : Fragment("Chat Settings") {

    val session: AgentChatSession by param()

    private val modelId = SimpleStringProperty(session.config.modelId)
    private val temperature = SimpleDoubleProperty(session.config.temperature)
    private val maxTokens = SimpleIntegerProperty(session.config.maxTokens)
    private val maxContextMessages = SimpleIntegerProperty(session.config.maxContextMessages)
    private val systemMessage = SimpleStringProperty(session.config.systemMessage ?: "")
    private val enableTools = SimpleBooleanProperty(session.config.enableTools)

    override val root = borderpane {
        prefWidth = 480.0
        center = form {
            padding = insets(15.0)
            fieldset("Model") {
                field("Model") {
                    val models = PromptFxModels.multimodalModels().map { it.modelId }
                    if (models.isEmpty()) {
                        textfield(modelId)
                    } else {
                        combobox(modelId, models) { isEditable = true }
                    }
                }
            }
            fieldset("Generation Parameters") {
                field("Temperature") {
                    hbox(8.0, Pos.CENTER_LEFT) {
                        slider(0.0..2.0) {
                            valueProperty().bindBidirectional(temperature)
                            prefWidth = 200.0
                        }
                        label { textProperty().bind(temperature.asString("%.2f")) }
                    }
                }
                field("Max Tokens") {
                    spinner(100, 128000, maxTokens.value, 100, true).apply {
                        prefWidth = 120.0
                        valueProperty().addListener { _, _, v -> maxTokens.set(v) }
                    }
                }
                field("Max Context Messages") {
                    spinner(1, 500, maxContextMessages.value, 10, true).apply {
                        prefWidth = 120.0
                        valueProperty().addListener { _, _, v -> maxContextMessages.set(v) }
                    }
                }
            }
            fieldset("Context") {
                field("System Message") {
                    labelContainer.alignment = Pos.TOP_LEFT
                    textarea(systemMessage) {
                        promptText = "Optional system message for this session"
                        prefRowCount = 4
                        isWrapText = true
                        prefWidth = 320.0
                    }
                }
                field("Enable Tools") {
                    checkbox("", enableTools)
                }
            }
        }
        bottom = hbox(10.0, Pos.CENTER_RIGHT) {
            padding = insets(10.0)
            button("OK") {
                isDefaultButton = true
                action {
                    session.config = AgentChatConfig(
                        modelId = modelId.value.trim(),
                        temperature = temperature.value,
                        maxTokens = maxTokens.value,
                        maxContextMessages = maxContextMessages.value,
                        systemMessage = systemMessage.value.trim().ifBlank { null },
                        enableTools = enableTools.value
                    )
                    close()
                }
            }
            button("Cancel") {
                isCancelButton = true
                action { close() }
            }
        }
    }
}

/** A single displayable message in the agent chat. */
class AgentMessageEntry(
    val type: MessageType,
    val author: String,
    text: String = "",
    val imageUri: URI? = null
) {
    /** Mutable text property so streaming and thinking-text updates are reflected in the UI. */
    val textProperty = SimpleStringProperty(text)
    var text: String
        get() = textProperty.value
        set(value) { textProperty.set(value) }
}

/** Types of messages displayed in the agent chat. */
enum class MessageType {
    /** A message sent by the user. */
    USER,
    /** A response from the assistant model. */
    ASSISTANT,
    /** A transient thinking/progress placeholder while the model is responding. */
    THINKING,
    /** An intermediate step (tool call, reasoning, planning). */
    INTERMEDIATE,
    /** An error message. */
    ERROR
}
