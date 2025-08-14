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
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.core.*
import tri.ai.pips.AiPipelineResult
import tri.ai.pips.asPipelineResult
import tri.util.ifNotBlank

/**
 * Advanced version of chat API, with support for tools.
 * See https://beta.openai.com/docs/api-reference/chat for more information.
 */
class ChatViewAdvanced : ChatView(
    "Chat (Advanced)",
    "Test the AI Assistant chat, with optional function calls and full control over chat history.",
    listOf(MChatRole.System, MChatRole.User, MChatRole.Assistant, MChatRole.Tool),
    showInput = true
) {

    private val toolsVisible = SimpleBooleanProperty(true)
    private val toolCall = SimpleStringProperty("")

    private lateinit var toolView: ToolListView

    init {
        input {
            hbox {
                alignment = Pos.CENTER
                checkbox("Tools", toolsVisible)
                tooltip("A list of functions the model may generate JSON inputs for.")
                region { hgrow = Priority.ALWAYS }
                combobox(toolCall, listOf("", "auto", "none")) {
                    promptText = "Function call"
                    isEditable = true
                    tooltip("""Specify name to call a specific function, or "none" (don't call any functions) or "auto" (model picks whether to call a function).""")
                }
            }
            toolView = ToolListView()
            toolView.root.visibleWhen(toolsVisible)
            add(toolView)
        }
    }

    override suspend fun processUserInput(): AiPipelineResult<MultimodalChatMessage> {
        val systemMessage = if (system.value.isNullOrBlank()) listOf() else
            listOf(MultimodalChatMessage.text(MChatRole.System, system.value))
        val messages = systemMessage + chatHistory.chatMessages().takeLast(messageHistory.value)
        val toolChoice = toolCall.value.ifNotBlank {
            when (it) {
                "auto" -> MToolChoice.AUTO
                "none" -> MToolChoice.NONE
                else -> MToolChoice.function(it)
            }
        }
        val tools = if (toolChoice == MToolChoice.NONE) {
            null
        } else {
            toolView.tools().ifEmpty { null }
        }
        val params = MChatParameters(
            variation = MChatVariation(
                seed = if (seedActive.value) seed.value else null,
                temperature = common.temp.value,
                topP = common.topP.value,
                frequencyPenalty = common.freqPenalty.value,
                presencePenalty = common.presPenalty.value
            ),
            tokens = common.maxTokens.value,
            stop = if (common.stopSequences.value.isBlank()) null else common.stopSequences.value.split("||"),
            responseFormat = responseFormat.value,
            numResponses = common.numResponses.value,
            tools = if (toolChoice != null && tools != null) MChatTools(toolChoice, tools) else null
        )

        val m = model.value!!
        val result = m.chat(messages, params)
        return result.asPipelineResult()
    }

}

