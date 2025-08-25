/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
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
package tri.promptfx.agent

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.TextArea
import javafx.scene.layout.Priority
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.core.MChatRole
import tri.ai.core.MultimodalChatMessage
import tri.ai.pips.agent.AgentChatAPI
import tri.ai.pips.agent.AgentChatConfig
import tri.ai.pips.agent.AgentChatSession
import tri.ai.pips.agent.DefaultAgentChatAPI
import tri.promptfx.PromptFxModels
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance
import tri.util.ui.graphic

/** Plugin for the [AgentChatView]. */
class AgentChatPlugin : NavigableWorkspaceViewImpl<AgentChatView>("Agents", "Agent Chat", WorkspaceViewAffordance.INPUT_ONLY, AgentChatView::class)

/** 
 * Agent chat view with modern chat interface design.
 * Provides contextual chat with reasoning capabilities.
 */
class AgentChatView : Fragment("Agent Chat") {

    private val api: AgentChatAPI = DefaultAgentChatAPI()
    private val currentSession = SimpleObjectProperty<AgentChatSession>()
    private val inputText = SimpleStringProperty("")
    private val isProcessing = SimpleObjectProperty(false)
    
    // UI Components
    private lateinit var chatHistoryArea: TextArea
    private lateinit var inputArea: TextArea
    private lateinit var sendButton: Button
    
    init {
        // Create default session
        currentSession.value = api.createSession(
            AgentChatConfig(
                modelId = PromptFxModels.policy.multimodalModelDefault()?.modelId ?: "gpt-4o-mini"
            )
        )
    }

    override val root = borderpane {
        prefWidth = 800.0
        prefHeight = 600.0
        
        // Chat history sidebar (left)
        left = vbox {
            prefWidth = 200.0
            padding = insets(10.0)
            spacing = 5.0
            
            label("Chat Sessions") {
                style {
                    fontSize = 14.px
                    fontWeight = javafx.scene.text.FontWeight.BOLD
                }
            }
            
            button("New Chat") {
                graphic = FontAwesomeIcon.PLUS.graphic
                maxWidth = Double.MAX_VALUE
                action { startNewChat() }
            }
            
            separator()
            
            // Sessions list would go here - simplified for now
            label("Current Session") {
                style {
                    fontSize = 12.px
                }
            }
        }
        
        // Main chat area (center)
        center = vbox {
            padding = insets(10.0)
            spacing = 10.0
            
            // Chat history display
            chatHistoryArea = textarea {
                isEditable = false
                isWrapText = true
                vgrow = Priority.ALWAYS
                prefRowCount = 20
                style {
                    fontSize = 13.px
                }
            }
            
            // Chat input area
            hbox {
                spacing = 10.0
                alignment = Pos.BOTTOM_CENTER
                
                inputArea = textarea(inputText) {
                    hgrow = Priority.ALWAYS
                    prefRowCount = 3
                    isWrapText = true
                    promptText = "Type your message here..."
                    
                    // Send on Ctrl+Enter
                    setOnKeyPressed { event ->
                        if (event.isControlDown && event.code == javafx.scene.input.KeyCode.ENTER) {
                            sendMessage()
                        }
                    }
                }
                
                sendButton = button("Send") {
                    graphic = FontAwesomeIcon.PAPER_PLANE.graphic
                    prefHeight = 80.0
                    isDefaultButton = true
                    enableWhen(inputText.isNotEmpty)
                    action { sendMessage() }
                }
            }
        }
        
        // Settings/config area (right) - simplified for now
        right = vbox {
            prefWidth = 150.0
            padding = insets(10.0)
            spacing = 5.0
            
            label("Settings") {
                style {
                    fontSize = 14.px
                    fontWeight = javafx.scene.text.FontWeight.BOLD
                }
            }
            
            button("Chat Settings") {
                graphic = FontAwesomeIcon.COG.graphic
                maxWidth = Double.MAX_VALUE
                action { showChatSettings() }
            }
        }
    }
    
    private fun startNewChat() {
        val config = currentSession.value?.config ?: AgentChatConfig()
        currentSession.value = api.createSession(config)
        updateChatHistory()
    }
    
    private fun sendMessage() {
        val message = inputText.value.trim()
        if (message.isEmpty() || isProcessing.value) return
        
        val session = currentSession.value ?: return
        val userMessage = MultimodalChatMessage.text(MChatRole.User, message)
        
        // Clear input and show processing state
        inputText.value = ""
        isProcessing.value = true
        
        runAsync {
            runBlocking {
                try {
                    api.sendMessage(session, userMessage)
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw e
                }
            }
        } ui { response ->
            isProcessing.value = false
            updateChatHistory()
        } fail { error ->
            isProcessing.value = false
            error.printStackTrace()
            // Add error message to chat
            val errorMsg = MultimodalChatMessage.text(
                MChatRole.Assistant, 
                "Error: ${error.message ?: "An unknown error occurred"}"
            )
            api.addMessage(session, errorMsg)
            updateChatHistory()
        }
    }
    
    private fun updateChatHistory() {
        val session = currentSession.value ?: return
        val history = StringBuilder()
        
        session.messages.forEach { message ->
            val role = when (message.role) {
                MChatRole.User -> "You"
                MChatRole.Assistant -> "Assistant"
                MChatRole.System -> "System"
                MChatRole.Tool -> "Tool"
            }
            val content = message.content?.firstOrNull()?.text ?: ""
            history.append("[$role]: $content\n\n")
        }
        
        if (isProcessing.value) {
            history.append("[Assistant]: Thinking...\n\n")
        }
        
        chatHistoryArea.text = history.toString()
        // Scroll to bottom
        chatHistoryArea.positionCaret(chatHistoryArea.text.length)
    }
    
    private fun showChatSettings() {
        // TODO: Implement settings dialog
        information("Settings", "Chat settings dialog will be implemented in a future update.")
    }
    
    override fun onDock() {
        updateChatHistory()
        inputArea.requestFocus()
    }
}