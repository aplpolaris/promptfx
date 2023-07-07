package tri.promptfx.api

import com.aallam.openai.api.edits.EditsRequest
import com.aallam.openai.api.model.ModelId
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.pips.AiPipelineResult
import tri.ai.openai.editsModels
import tri.promptfx.AiTaskView
import tri.promptfx.CommonParameters

class EditsView : AiTaskView("Edit", "Enter text to edit, and instructions to apply to the text below.") {

    private val input = SimpleStringProperty("")
    private val instructText = SimpleStringProperty("")
    private val model = SimpleStringProperty(editsModels[0])
    private val length = SimpleIntegerProperty(50)
    private var common = CommonParameters()

    init {
        addInputTextArea(input)
        input {
            text("Instructions")
            textarea(instructText) {
                isWrapText = true
            }
        }
    }

    init {
        parameters("Edits") {
            field("Model") {
                combobox(model, editsModels)
            }
        }
        parameters("Parameters") {
            with(common) {
                temperature()
                topP()
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

    override suspend fun processUserInput(): AiPipelineResult {
        val request = EditsRequest(
            model = ModelId(model.value),
            input = input.get(),
            instruction = instructText.get(),
            temperature = common.temp.value,
            topP = common.topP.value
        )
        return controller.openAiPlugin.client.edit(request).asPipelineResult()
    }

}
