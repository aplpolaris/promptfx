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
package tri.promptfx.`fun`

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.core.TextChatMessage
import tri.ai.core.MChatRole
import tri.ai.pips.AiPlanner
import tri.ai.pips.aitask
import tri.ai.prompt.PromptLibrary
import tri.ai.prompt.trace.AiPromptTrace
import tri.promptfx.AiPlanTaskView
import tri.promptfx.PromptFx
import tri.promptfx.PromptFxGlobals.fillPrompt
import tri.promptfx.ui.ChatEntry
import tri.promptfx.ui.ChatPanel
import tri.promptfx.ui.PromptSelectionModel
import tri.promptfx.ui.promptfield
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.graphic

/** Plugin for the [ChatBackView]. */
class ChatBackPlugin : NavigableWorkspaceViewImpl<ChatBackView>("Fun", "AI Conversations", type = ChatBackView::class)

/** View with prompts for configuring a topical conversation among various personalities. */
class ChatBackView : AiPlanTaskView("AI Chatting with Itself", "Enter a starting prompt and/or add to conversation below.") {

    private var maxTokens = SimpleIntegerProperty(500)
    private val maxMessageHistory = SimpleIntegerProperty(10)

    private val peopleOptions = SimpleStringProperty("Alice, Barry, Craig")
    private val conversationTopic = SimpleStringProperty("dogs")
    private val conversationSetting = SimpleStringProperty("at a park")
    private val conversationTone = SimpleStringProperty("friendly")
    private val conversationScript = SimpleStringProperty("in a movie script")

    private val peopleList
        get() = peopleOptions.value.split(",").map { it.trim() }

    private val userPerson = SimpleStringProperty("Alice")
    private val userInput = SimpleStringProperty("")

    private val history = ChatBackHistory()
    private val chatHistory = observableListOf<ChatEntry>()

    //region TRACKING PERSONS - CURRENT AND NEXT

    init {
        peopleOptions.onChange {
            userPerson.set(peopleList[0])
            history.clear()
            updateHistoryView()
        }
    }

    private val nextPerson
        get() = history.lastPerson?.nextPerson() ?: userPerson.get()

    private val otherPersons
        get() = (peopleList - nextPerson).joinToString(" and ")

    private fun String.nextPerson(): String {
        val list = peopleList
        val index = list.indexOf(this)
        return list[(index + 1) % peopleList.size]
    }

    //endregion

    init {
        input {
            style { fontSize = 14.px }
            toolbar {
                text("Chat as ")
                combobox(userPerson, peopleList) {
                    peopleOptions.onChange {
                        items.setAll(peopleList)
                        selectionModel.select(items.first())
                    }
                }
            }
            textarea(userInput) {
                isWrapText = true
            }
            toolbar {
                text("Chat History:")
                spacer()
                button("", FontAwesomeIcon.TRASH.graphic) {
                    action {
                        history.clear()
                        updateHistoryView()
                    }
                }
            }
            find<ChatPanel>().apply {
                chats.bind(chatHistory) { it }
                root.attachTo(this@input)
            }
        }
        parameters("Conversation") {
            field("People: ") {
                textfield(peopleOptions)
            }
            field("talking about:") {
                textfield(conversationTopic)
            }
            field("taking place:") {
                textfield(conversationSetting)
            }
            field("with tone:") {
                textfield(conversationTone)
            }
            field("what they might say:") {
                textfield(conversationScript)
            }
        }
        parameters("Chat Options") {
            field("Conversation history size") {
                tooltip("How many messages to keep in the conversation history.")
                slider(1..50) {
                    valueProperty().bindBidirectional(maxMessageHistory)
                }
                label(maxMessageHistory)
            }
            field("Max tokens") {
                tooltip("Max # of tokens for combined query/response from the question answering engine")
                slider(0..1000) {
                    valueProperty().bindBidirectional(maxTokens)
                }
                label(maxTokens.asString())
            }
            promptfield(prompt = PromptSelectionModel("chat/chat-back"), workspace = workspace)
        }
    }

    init {
        onCompleted {
            val response = it.finalResult.firstValue.toString()
            val person = peopleList.firstOrNull { response.startsWith("$it:") }
            if (person != null) {
                val message = response.substringAfter(":").trim()
                history.conversations.add(ChatEntry(person, message))
            } else {
                history.conversations.add(ChatEntry(nextPerson, response))
            }
            updateHistoryView()
        }
    }

    override fun plan(): AiPlanner {
        addUserInputToHistory()
        return aitask("chat-back") {
            chatBack()
        }.planner
    }

    private fun addUserInputToHistory() {
        val person = userPerson.value
        val input = userInput.value
        if (input.isNotBlank()) {
            history.conversations.add(ChatEntry(person, input))
            userInput.set("")
        }
    }

    private fun updateHistoryView() {
        chatHistory.setAll(history.conversations)
    }

    private suspend fun chatBack(): AiPromptTrace<String> {
        val systemMessage = fillPrompt("chat-back",
            "person" to nextPerson,
            "other persons" to otherPersons,
            "topic" to conversationTopic.value,
            "setting" to conversationSetting.value,
            "tone" to conversationTone.value,
            "script" to conversationScript.value
        )
        return controller.chatService.value.chat(
            listOf(TextChatMessage(MChatRole.System, systemMessage)) +
                history.toChatMessages(nextPerson, otherPersons, maxMessageHistory.value),
            tokens = maxTokens.value,
            stop = listOf("\n")
        ).mapOutput { it.content!! }
    }

}

/** Tracks conversation among multiple parties over time. */
private class ChatBackHistory {

    /** Key is person, value is what they said. */
    val conversations = mutableListOf<ChatEntry>()

    /** Last person to speak. */
    val lastPerson
        get() = conversations.lastOrNull()?.user

    /** Convert to AI message list, with given user mapping. */
    fun toChatMessages(nextPerson: String, otherPersons: String, maxMessageHistory: Int) =
        conversations.takeLast(maxMessageHistory).map {
            val role = if (it.user == nextPerson) MChatRole.Assistant else MChatRole.User
            TextChatMessage(role, "${it.user}: ${it.message}")
        }

    fun clear() {
        conversations.clear()
    }
}
