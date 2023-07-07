package tri.promptfx.apps

import javafx.beans.property.SimpleStringProperty
import tornadofx.combobox
import tornadofx.field
import tri.ai.openai.instructTextPlan
import tri.promptfx.*
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.yaml

class TranslationView: AiPlanTaskView("Translation", "Enter text to translate") {

    private val modeOptions = resources.yaml("resources/modes.yaml")["translation"] as List<String>
    private val mode = SimpleStringProperty(modeOptions[0])
    private val sourceText = SimpleStringProperty("")

    init {
        addInputTextArea(sourceText)
        parameters("Target Language") {
            field("Language") {
                combobox(mode, modeOptions)
            }
        }
    }

    override fun plan() = completionEngine.instructTextPlan("translate-text",
        instruct = mode.get(),
        userText = sourceText.get(),
        tokenLimit = 1000)

}

class TranslationPlugin : NavigableWorkspaceViewImpl<TranslationView>("Text", "Translation", TranslationView::class)