package tri.promptfx.`fun`

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.openai.chatModels
import tri.ai.pips.AiPlanner
import tri.ai.pips.AiTask
import tri.ai.pips.AiTaskResult
import tri.ai.pips.aitask
import tri.promptfx.AiPlanTaskView
import tri.promptfx.CommonParameters
import tri.util.ui.graphic

@OptIn(BetaOpenAI::class)
class ChatBackView : AiPlanTaskView("AI Chatting with Itself", "Enter a starting prompt and/or add to conversation below.") {

    private val model = SimpleStringProperty(chatModels[0])
    private var common = CommonParameters()
    private val maxMessageHistory = SimpleIntegerProperty(10)

    private val peopleOptions = SimpleStringProperty("Alice, Barry, Craig")
    private val conversationTopic = SimpleStringProperty("dogs")
    private val conversationSetting = SimpleStringProperty("at a park")
    private val conversationTone = SimpleStringProperty("friendly")

    private val peopleList
        get() = peopleOptions.value.split(",").map { it.trim() }

    private val userPerson = SimpleStringProperty("Alice")
    private val userInput = SimpleStringProperty("")

    private val history = ChatBackHistory()
    private val chatHistory = SimpleStringProperty("")

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
            padding = insets(5.0)
            spacing = 5.0
            style { fontSize = 14.px }
            hbox {
                style { alignment = Pos.CENTER_LEFT }
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
            hbox {
                style { alignment = Pos.CENTER_LEFT }
                text("Chat History:")
                region { hgrow = Priority.ALWAYS }
                button("", FontAwesomeIcon.TRASH.graphic) {
                    action {
                        history.clear()
                        updateHistoryView()
                    }
                }
            }
            // TODO - make this a listview with a custom cell factory
            textarea(chatHistory) {
                vgrow = Priority.ALWAYS
                isWrapText = true
                isEditable = false
                style { fontSize = 18.px }
                chatHistory.onChange {
                    appendText("")
                }
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
        }
        parameters("Chat Model") {
            field("Model") {
                combobox(model, chatModels)
            }
            field("Max History Size") {
                slider(1..50) {
                    valueProperty().bindBidirectional(maxMessageHistory)
                }
                label(maxMessageHistory)
            }
        }
        parameters("Parameters") {
            with(common) {
                temperature()
                topP()
                frequencyPenalty()
                presencePenalty()
            }
        }
    }

    init {
        onCompleted {
            val response = it.finalResult.toString()
            if (response.startsWith("$nextPerson:")) {
                val (person, message) = response.split(": ", limit = 2)
                history.conversations.add(person to message)
            } else {
                history.conversations.add(nextPerson to response)
            }
            updateHistoryView()
        }
    }

    override fun plan() = object : AiPlanner {
        override fun plan(): List<AiTask<*>> {
            addUserInputToHistory()
            updateHistoryView()

            return aitask("chat-back") {
                chatBack()
            }.plan
        }
    }

    private fun addUserInputToHistory() {
        val person = userPerson.value
        val input = userInput.value
        if (input.isNotBlank()) {
            history.conversations.add(person to input)
            userInput.set("")
        }
    }

    private fun updateHistoryView() {
        chatHistory.set(history.conversations.joinToString("\n\n") { "${it.first}: ${it.second}" })
    }

    private suspend fun chatBack(): AiTaskResult<String> {
        val systemMessage = "You are role-playing as $nextPerson " +
                "talking about ${conversationTopic.value} " +
                "with $otherPersons. " +
                "The conversation is taking place ${conversationSetting.value} " +
                "and has a ${conversationTone.value} tone."
        return controller.openAiPlugin.client.chatCompletion(ChatCompletionRequest(
            ModelId(model.value),
            listOf(ChatMessage(ChatRole.System, systemMessage)) +
                history.toChatMessages(nextPerson, otherPersons, maxMessageHistory.value),
            stop = listOf("\n")
        ))
    }

}

/** Tracks conversation among multiple parties over time. */
@OptIn(BetaOpenAI::class)
private class ChatBackHistory {

    /** Key is person, value is what they said. */
    val conversations = mutableListOf<PersonMessage>()

    /** Last person to speak. */
    val lastPerson
        get() = conversations.lastOrNull()?.first

    /** Convert to AI message list, with given user mapping. */
    fun toChatMessages(nextPerson: String, otherPersons: String, maxMessageHistory: Int) =
                conversations.takeLast(maxMessageHistory).map {
                    val role = if (it.first == nextPerson) ChatRole.Assistant else ChatRole.User
                    ChatMessage(role, "${it.first}: ${it.second}")
                }

    fun clear() {
        conversations.clear()
    }
}

typealias PersonMessage = Pair<String, String>
