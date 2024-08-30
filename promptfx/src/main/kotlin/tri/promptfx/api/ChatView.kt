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

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatResponseFormat
import com.aallam.openai.api.chat.ToolCall
import com.aallam.openai.api.chat.chatMessage
import com.aallam.openai.api.core.Role
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.gemini.Content
import tri.ai.gemini.GeminiModelIndex
import tri.ai.openai.OpenAiModelIndex
import tri.promptfx.AiTaskView
import tri.promptfx.ModelParameters

/**
 * Common functionality for chat API views.
 * See https://beta.openai.com/docs/api-reference/chat for more information.
 */
abstract class ChatView(title: String, instruction: String, private val roles: List<Role>, showInput: Boolean) : AiTaskView(title, instruction, showInput) {

    private val chatModels = OpenAiModelIndex.chatModels(includeSnapshots = true) +
            GeminiModelIndex.chatModels(includeSnapshots = true)

    protected val system = SimpleStringProperty("")

    protected val model = SimpleStringProperty(chatModels.first())
    protected val messageHistory = SimpleIntegerProperty(10)

    protected val seedActive = SimpleBooleanProperty(false)
    protected val seed = SimpleIntegerProperty(0)
    protected val responseFormat = SimpleObjectProperty(ChatResponseFormat.Text)
    protected var common = ModelParameters()

    protected lateinit var chatHistory: ChatHistoryView

    init {
        initChatSystemMessage()
        initChatOutput()
        initChatParameters()
        initChatResponse()
    }

    //region INITIALIZATION

    private fun initChatSystemMessage() {
        output {
            spacing = 5.0
            getChildList()!!.clear()
            textarea(system) {
                promptText = "Optional system message to include in chat history"
                prefRowCount = 3
                isWrapText = true
            }
        }
    }

    private fun initChatOutput() {
        output {
            chatHistory = ChatHistoryView(roles)
            add(chatHistory)
        }
    }

    private fun initChatParameters() {
        parameters("Chat Model") {
            field("Model") {
                combobox(model, chatModels)
            }
        }
        parameters("Chat Input") {
            field("Message History") {
                slider(1..100) {
                    valueProperty().bindBidirectional(messageHistory)
                }
                label(messageHistory.asString())
            }
        }
        parameters("Sampling Parameters") {
            with(common) {
                temperature()
                topP()
                frequencyPenalty()
                presencePenalty()
            }
        }
        parameters("Chat Output") {
            with (common) {
                maxTokens()
                stopSequences()
            }
            field("Seed") {
                tooltip("If specified, our system will make a best effort to sample deterministically, such that repeated requests with the same seed and parameters should return the same result.")
                checkbox("Active", seedActive)
                textfield(seed) { enableWhen(seedActive) }
            }
            field("Response Format") {
                tooltip("Important: when using JSON mode, you must also instruct the model to produce JSON yourself via a system or user message.")
                combobox(responseFormat, listOf(ChatResponseFormat.Text, ChatResponseFormat.JsonObject)) {
                    cellFormat { text = it.type }
                }
            }
            with (common) {
                numResponses()
            }
        }
    }

    private fun initChatResponse() {
        onCompleted {
            it.finalResult?.let {
                addChatsToHistory(it)
            }
        }
    }

    /** Add chats to history, also add follow-up chats for testing if relevant, and a subsequent user message. */
    private fun addChatsToHistory(it: Any) {
        when {
            it is ChatMessage -> addChat(it)
            it is Content -> addChat(it.toChatMessage())
            it is List<*> && it.all { it is ChatMessage } -> addChatChoices(it.map { it as ChatMessage })
            it is List<*> -> it.forEach { addChatsToHistory(it!!) }
            else -> addChat(ChatMessage(Role.Assistant, it.toString()))
        }
    }

    private fun Content.toChatMessage() = chatMessage {
        role = when (this@toChatMessage.role) {
            "user" -> Role.User
            "model" -> Role.Assistant
            else -> throw IllegalArgumentException("Unsupported role: $role")
        }
        if (parts.size == 1 && parts[0].text != null && parts[0].inlineData == null) {
            content = parts[0].text!!
        } else {
            content {
                parts.forEach {
                    when {
                        it.text != null && it.inlineData != null -> throw IllegalStateException("Unsupported content: $it")
                        it.text != null -> this@content.text(it.text!!)
                        it.inlineData != null -> this@content.image(it.inlineData!!.data)
                        else -> throw IllegalStateException("Unsupported content: $it")
                    }
                }
            }
        }
    }

    private fun addChat(chat: ChatMessage) {
        chatHistory.components.add(ChatMessageUiModel.valueOf(chat))
        val askTools = chat.toolCalls != null
        if (askTools) {
            chat.toolCalls!!.forEach {
                // add response placeholder for each tool
                val sampleResponse = ChatMessage.Tool("(replace this with tool response)", (it as ToolCall.Function).id)
                chatHistory.components.add(ChatMessageUiModel.valueOf(sampleResponse))
            }
        } else {
            // add blank message for user to follow up
            chatHistory.components.add(ChatMessageUiModel(Role.User, ""))
        }
    }

    /** Adds multiple chat messages as different options for user. */
    private fun addChatChoices(chat: List<ChatMessage>) {
        chatHistory.components.add(ChatMessageUiModel.valueOf(chat))
        val toolCalls = chat.first().toolCalls
        if (toolCalls != null) {
            toolCalls.forEach {
                // add response placeholder for each tool
                val sampleResponse = ChatMessage.Tool("(replace this with tool response)", (it as ToolCall.Function).id)
                chatHistory.components.add(ChatMessageUiModel.valueOf(sampleResponse))
            }
        } else {
            // add blank message for user to follow up
            chatHistory.components.add(ChatMessageUiModel(Role.User, ""))
        }
    }

    //endregion

}
