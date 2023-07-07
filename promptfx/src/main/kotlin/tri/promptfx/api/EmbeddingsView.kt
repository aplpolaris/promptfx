package tri.promptfx.api

import javafx.beans.property.SimpleStringProperty
import tornadofx.combobox
import tornadofx.field
import tri.ai.openai.embeddingsModels
import tri.ai.pips.AiPipelineResult
import tri.promptfx.AiTaskView

class EmbeddingsView : AiTaskView("Embeddings", "Enter text to calculate embedding (each line will be calculated separately).") {

    private val input = SimpleStringProperty("")
    private val model = SimpleStringProperty(embeddingsModels[0])

    init {
        addInputTextArea(input)
        parameters("Embeddings") {
            field("Model") {
                combobox(model, embeddingsModels)
            }
        }
        val outputEditor = outputPane.lookup(".text-area") as javafx.scene.control.TextArea
        outputEditor.isWrapText = false
    }

    override suspend fun processUserInput(): AiPipelineResult {
        val inputs = input.get().split("\n").filter { it.isNotBlank() }
        return controller.openAiClient.quickEmbedding(model.value, inputs).map {
            it.joinToString("\n") { it.joinToString(",", prefix = "[", postfix = "]") { it.format(3) } }
        }.asPipelineResult()
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

}
