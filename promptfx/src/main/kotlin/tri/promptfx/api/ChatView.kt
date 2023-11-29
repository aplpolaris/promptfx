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

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatResponseFormat
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.openai.OpenAiModels
import tri.promptfx.AiTaskView
import tri.promptfx.ModelParameters

/**
 * Common functionality for chat API views.
 * See https://beta.openai.com/docs/api-reference/chat for more information.
 */
abstract class ChatView(title: String, instruction: String) : AiTaskView(title, instruction) {

    private val CHAT_MODELS = OpenAiModels.chatModels(includeSnapshots = true)

    protected val system = SimpleStringProperty("")

    protected val model = SimpleStringProperty(CHAT_MODELS.first())
    protected val messageHistory = SimpleIntegerProperty(10)

    protected val seedActive = SimpleBooleanProperty(false)
    protected val seed = SimpleIntegerProperty(0)
    protected val stopSequences = SimpleStringProperty("")
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
        input {
            spacing = 10.0
            paddingAll = 10.0
            text("System message:")
            textarea(system) {
                vgrow = Priority.ALWAYS
                isWrapText = true
            }
        }
    }

    private fun initChatOutput() {
        output {
            paddingAll = 10.0
            getChildList()!!.clear()
            chatHistory = ChatHistoryView()
            add(chatHistory)
        }
    }

    fun initChatParameters() {
        parameters("Chat Model") {
            field("Model") {
                combobox(model, CHAT_MODELS)
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
            }
            field("Stop Sequences") {
                tooltip("A list of up to 4 sequences where the API will stop generating further tokens. Use || to separate sequences.")
                textfield(stopSequences)
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
        }
    }

    fun initChatResponse() {
        onCompleted {
            it.finalResult?.let {
                with (chatHistory.components) {
                    when (it) {
                        is ChatMessage -> add(ChatLineModel.valueOf(it))
                        else -> add(ChatLineModel(com.aallam.openai.api.chat.ChatRole.Assistant, it.toString()))
                    }
                    add(ChatLineModel(com.aallam.openai.api.chat.ChatRole.User, ""))
                }
            }
        }
    }

    //endregion

}
