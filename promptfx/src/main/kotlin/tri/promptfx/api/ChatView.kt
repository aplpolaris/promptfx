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

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.pips.AiPipelineResult
import tri.ai.openai.OpenAiSettings
import tri.ai.openai.chatModels
import tri.ai.pips.AiTaskResult.Companion.result
import tri.promptfx.AiTaskView
import tri.promptfx.ChatHistoryView
import tri.promptfx.ChatLineModel
import tri.promptfx.CommonParameters

@OptIn(BetaOpenAI::class)
class ChatView : AiTaskView("Chat", "You are chatting with an AI Assistant.") {

    private val system = SimpleStringProperty("")
    private lateinit var chatHistory: ChatHistoryView
    private val model = SimpleStringProperty(chatModels[0])
    private val messageHistory = SimpleIntegerProperty(10)
    private val length = SimpleIntegerProperty(50)
    private var common = CommonParameters()

    init {
        input {
            text("Enter system message")
        }
        addInputTextArea(system)
        output {
            getChildList()!!.clear()
            chatHistory = ChatHistoryView()
            add(chatHistory)
        }
        parameters("Chat Model") {
            field("Model") {
                combobox(model, chatModels)
            }
        }
        parameters("Parameters") {
            with(common) {
                temperature()
                topP()
                frequencyPenalty()
                presencePenalty()
            }
            field("Message History") {
                slider(1..100) {
                    valueProperty().bindBidirectional(messageHistory)
                }
            }
        }
        parameters("Output") {
            field("Maximum Length") {
                slider(0..500) {
                    valueProperty().bindBidirectional(length)
                }
                label(length.asString())
            }
        }
    }

    init {
        onCompleted {
            chatHistory.components.add(ChatLineModel(ChatRole.Assistant, it.finalResult.toString()))
            chatHistory.components.add(ChatLineModel(ChatRole.User, ""))
        }
    }

    override suspend fun processUserInput(): AiPipelineResult {
        val completion = ChatCompletionRequest(
            model = ModelId(model.value),
            messages = listOf(ChatMessage(ChatRole.System, system.get())) +
                    chatHistory.chatMessages().takeLast(messageHistory.value),
            temperature = common.temp.value,
            topP = common.topP.value,
            frequencyPenalty = common.freqPenalty.value,
            presencePenalty = common.presPenalty.value,
            maxTokens = length.value,
        )
        return controller.openAiPlugin.client.chatCompletion(completion).asPipelineResult()
    }

}
