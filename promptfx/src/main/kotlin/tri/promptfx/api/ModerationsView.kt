package tri.promptfx.api

import com.aallam.openai.api.moderation.ModerationModel
import com.aallam.openai.api.moderation.ModerationRequest
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.pips.AiPipelineResult
import tri.ai.openai.OpenAiSettings
import tri.ai.openai.mapper
import tri.ai.pips.AiTaskResult.Companion.result
import tri.promptfx.AiTaskView

class ModerationsView : AiTaskView("Moderations", "Enter text to generate moderation scores") {

    private val input = SimpleStringProperty("")
    private val model = SimpleObjectProperty(ModerationModel.Latest)

    init {
        addInputTextArea(input)
        parameters("Moderations") {
            field("Model") {
                combobox(model, listOf(ModerationModel.Latest, ModerationModel.Stable))
            }
        }
    }

    override suspend fun processUserInput(): AiPipelineResult {
        val request = ModerationRequest(
            input = listOf(input.value),
            model = model.value
        )
        val response = controller.openAiPlugin.client.client.moderations(request)
        val responseText = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response)
        return result(responseText, model.value.model).asPipelineResult()
    }

}