package tri.promptfx.ui

import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.prompt.trace.AiPromptTrace

/** A card that displays the trace of a prompt. */
class PromptTraceCard : Fragment() {

    val prompt = SimpleStringProperty("")
    val promptParams = SimpleStringProperty("")
    val model = SimpleStringProperty("")
    val modelParams = SimpleStringProperty("")
    val exec = SimpleStringProperty("")
    val result = SimpleStringProperty("")

    fun setTrace(trace: AiPromptTrace) {
        prompt.value = trace.promptInfo.prompt
        promptParams.value = trace.promptInfo.promptParams.toString()
        model.value = trace.modelInfo.model
        modelParams.value = trace.modelInfo.modelParams.toString()
        exec.value = trace.execInfo.toString()
        result.value = trace.outputInfo.output
    }

    override val root = form {
        fieldset("Input") {
            field("Prompt") { textfield(prompt) }
            field("Prompt Params") { textfield(promptParams) }
            field("Model") { textfield(model) }
            field("Model Params") { textfield(modelParams) }
        }
        fieldset("Result") {
            field("Execution") { textfield(exec) }
            field("Result") { textarea(result) }
        }
    }
}