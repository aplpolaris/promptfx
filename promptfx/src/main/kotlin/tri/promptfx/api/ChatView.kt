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
package tri.promptfx.api

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.core.*
import tri.promptfx.AiTaskView
import tri.promptfx.ModelParameters
import tri.promptfx.PromptFxModels

/**
 * Common functionality for chat API views.
 * See https://beta.openai.com/docs/api-reference/chat for more information.
 */
abstract class ChatView(title: String, instruction: String, private val roles: List<MChatRole>, showInput: Boolean) : AiTaskView(title, instruction, showInput) {

    private val chatModels = PromptFxModels.sourcedMultimodalModels()
    protected val system = SimpleStringProperty("")
    protected val model = SimpleObjectProperty(chatModels.firstOrNull())
    protected val messageHistory = SimpleIntegerProperty(10)

    protected val seedActive = SimpleBooleanProperty(false)
    protected val seed = SimpleIntegerProperty(0)
    protected val responseFormat = SimpleObjectProperty(MResponseFormat.TEXT)
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
                combobox(responseFormat, MResponseFormat.values().toList())
            }
            with (common) {
                numResponses()
            }
        }
    }

    private fun initChatResponse() {
        onCompleted {
            addChatsToHistory(it.finalResult.output?.outputs?.map { it.content() } ?: listOf())
        }
    }

    /** Add chats to history, also add follow-up chats for testing if relevant, and a subsequent user message. */
    private fun addChatsToHistory(results: List<Any?>) {
        val types = results.mapNotNull { it?.javaClass }.toSet()
        when {
            types == setOf(MultimodalChatMessage::class.java) -> addChatChoices(results as List<MultimodalChatMessage>)
            types == setOf(String::class.java) -> results.map { MultimodalChatMessage.text(MChatRole.Assistant, it.toString()) }.forEach { addChat(it) }
            types.isEmpty() -> tri.util.warning<ChatView>("No chat responses found in output.")
            else -> throw IllegalArgumentException("Unsupported chat response type: $types")
        }
    }

    private fun addChat(chat: MultimodalChatMessage) {
        chatHistory.components.add(ChatMessageUiModel.valueOf(chat))
        val askTools = chat.toolCalls?.isNotEmpty() == true
        if (askTools) {
            chat.toolCalls?.forEach {
                // add response placeholder for each tool
                val sampleResponse = MultimodalChatMessage.tool("(replace this with tool response)", it.id)
                chatHistory.components.add(ChatMessageUiModel.valueOf(sampleResponse))
            }
        } else {
            // add blank message for user to follow up
            chatHistory.components.add(ChatMessageUiModel(MChatRole.User, ""))
        }
    }

    /** Adds multiple chat messages as different options for user. */
    private fun addChatChoices(chat: List<MultimodalChatMessage>) {
        chatHistory.components.add(ChatMessageUiModel.valueOf(chat))
        val toolCalls = chat.first().toolCalls
        if (toolCalls?.isNotEmpty() == true) {
            toolCalls.forEach {
                // add response placeholder for each tool
                val sampleResponse = MultimodalChatMessage.tool("(replace this with tool response)", it.id.ifBlank { it.name })
                chatHistory.components.add(ChatMessageUiModel.valueOf(sampleResponse))
            }
        } else {
            // add blank message for user to follow up
            chatHistory.components.add(ChatMessageUiModel(MChatRole.User, ""))
        }
    }

    //endregion

}
