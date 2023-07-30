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
import com.aallam.openai.api.chat.FunctionMode
import com.aallam.openai.api.model.ModelId
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.pips.AiPipelineResult
import tri.util.ui.ifNotBlank

/**
 * Advanced version of chat API, with support for functions.
 * See https://beta.openai.com/docs/api-reference/chat for more information.
 */
@OptIn(BetaOpenAI::class)
class ChatViewAdvanced : ChatView("Chat (Advanced)", "You are chatting with an AI Assistant.") {

    private val functionVisible = SimpleBooleanProperty(false)
    private val functionCall = SimpleStringProperty("")

    private lateinit var functionView: FunctionListView

    init {
        input {
            hbox {
                alignment = Pos.CENTER
                checkbox("Functions", functionVisible)
                tooltip("A list of functions the model may generate JSON inputs for.")
                region { hgrow = Priority.ALWAYS }
                textfield(functionCall) {
                    tooltip("""Specify name to call a specific function, or "none" (don't call any functions) or "auto" (model picks whether to call a function).""")
                }
            }
            functionView = FunctionListView()
            functionView.root.visibleWhen(functionVisible)
            add(functionView)
        }
    }

    override suspend fun processUserInput(): AiPipelineResult {
        val systemMessage = if (system.value.isNullOrBlank()) listOf() else
            listOf(ChatMessage(ChatRole.System, system.value))
        val messages = systemMessage + chatHistory.chatMessages().takeLast(messageHistory.value)
        val functionMode = functionCall.value.ifNotBlank {
            when (it) {
                "auto" -> FunctionMode.Auto
                "none" -> FunctionMode.None
                else -> FunctionMode.Named(it)
            }
        }
        val functions = if (functionMode == FunctionMode.None) {
            null
        } else {
            functionView.functions().ifEmpty { null }
        }

        val completion = ChatCompletionRequest(
            model = ModelId(model.value),
            messages = messages,
            temperature = common.temp.value,
            topP = common.topP.value,
            n = null,
            stop = if (stopSequences.value.isBlank()) null else stopSequences.value.split("||"),
            maxTokens = maxTokens.value,
            presencePenalty = common.presPenalty.value,
            frequencyPenalty = common.freqPenalty.value,
            logitBias = null,
            user = null,
            functions = functions,
            functionCall = functionMode,
        )
        return controller.openAiPlugin.client.chat(completion).asPipelineResult()
    }

}

