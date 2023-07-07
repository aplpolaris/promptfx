package tri.promptfx.apps

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.combobox
import tornadofx.field
import tornadofx.label
import tornadofx.slider
import tri.ai.openai.templatePlan
import tri.promptfx.AiPlanTaskView
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.yaml

class EntityExtractionView: AiPlanTaskView("Entity Extraction", "Enter text to extract entities or facts") {

    private val modeOptions = resources.yaml("resources/modes.yaml")["entities"] as List<String>
    private val formatModeOptions = resources.yaml("resources/modes.yaml")["structured-format"] as Map<String, String>
    private val sourceText = SimpleStringProperty("")
    private val mode = SimpleStringProperty(modeOptions[0])
    private val formatMode = SimpleStringProperty(formatModeOptions.keys.first())
    private val length = SimpleIntegerProperty(300)

    init {
        addInputTextArea(sourceText)
        parameters("Extraction Mode") {
            field("Mode") {
                combobox(mode, modeOptions)
            }
            field("Format as") {
                combobox(formatMode, formatModeOptions.keys.toList())
            }
        }
        parameters("Output") {
            field("Maximum Length") {
                slider(0..2000) {
                    valueProperty().bindBidirectional(length)
                }
                label(length.asString())
            }
        }
    }

    override fun plan() = completionEngine.templatePlan("entity-extraction",
        "input" to sourceText.get(),
        "mode" to mode.value,
        "format" to formatModeOptions[formatMode.value]!!,
        tokenLimit = length.get()
    )

}

class EntityExtractionPlugin : NavigableWorkspaceViewImpl<EntityExtractionView>("Text", "Entity Extraction", EntityExtractionView::class)