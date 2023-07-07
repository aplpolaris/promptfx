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
        return controller.openAiClient.chatCompletion(completion).asPipelineResult()
    }

}
