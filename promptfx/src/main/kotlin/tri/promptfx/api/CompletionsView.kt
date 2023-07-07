package tri.promptfx.api

import com.aallam.openai.api.completion.CompletionRequest
import com.aallam.openai.api.model.ModelId
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.pips.AiPipelineResult
import tri.ai.openai.OpenAiSettings
import tri.ai.openai.completionModels
import tri.promptfx.AiTaskView
import tri.promptfx.CommonParameters

class CompletionsView : AiTaskView("Completion", "Enter text to complete") {

    private val input = SimpleStringProperty("")
    private val model = SimpleStringProperty(completionModels[0])
    private val length = SimpleIntegerProperty(50)
    private var common = CommonParameters()

    init {
        addInputTextArea(input)
        parameters("Completion Model") {
            field("Model") {
                combobox(model, completionModels)
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

    override suspend fun processUserInput(): AiPipelineResult {
        val completion = CompletionRequest(
            model = ModelId(model.value),
            prompt = input.get(),
            temperature = common.temp.value,
            topP = common.topP.value,
            frequencyPenalty = common.freqPenalty.value,
            presencePenalty = common.presPenalty.value,
            maxTokens = length.value,
        )
        return controller.openAiClient.completion(completion).asPipelineResult()
    }

}