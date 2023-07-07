package tri.promptfx.apps

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.openai.templatePlan
import tri.promptfx.AiPlanTaskView
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.yaml

class TextToJsonView: AiPlanTaskView("Text-to-JSON",
    "Enter text in the top box to convert to JSON (or other structured format).",) {

    private val sourceText = SimpleStringProperty("")

    private val formatModeOptions = resources.yaml("resources/modes.yaml")["structured-format"] as Map<String, String>
    private val guidance = SimpleStringProperty("")
    private val formatMode = SimpleStringProperty(formatModeOptions.keys.first())

    private val sampleOutput = SimpleStringProperty("")
    private val length = SimpleIntegerProperty(300)
//    private var common = CommonParameters()

    init {
        addInputTextArea(sourceText)
        input {
            label("Sample JSON (YAML, XML, CSV, ...):")
            textarea(sampleOutput) {
                isWrapText = true
            }
        }
        parameters("Extraction Mode") {
            field("Guidance") {
                tooltip("If this is not blank, adds 'The result should contain X' to the instruction.")
                textfield(guidance)
            }
            field("Format as") {
                combobox(formatMode, formatModeOptions.keys.toList())
            }
        }
//        parameters("Parameters") {
//            with(common) {
//                temperature()
//                topP()
//            }
//        }
        parameters("Output") {
            field("Maximum Length") {
                slider(0..2000) {
                    valueProperty().bindBidirectional(length)
                }
                label(length.asString())
            }
        }
    }

    override fun plan() = completionEngine.templatePlan("text-to-json",
        "input" to sourceText.get(),
        "guidance" to guidance.get(),
        "format" to formatModeOptions[formatMode.value]!!,
        "example" to sampleOutput.get(),
        tokenLimit = length.get()
    )

}

class TextToJsonPlugin : NavigableWorkspaceViewImpl<TextToJsonView>("Text", "Text-to-JSON", TextToJsonView::class)