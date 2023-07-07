package tri.promptfx.apps

import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.openai.instructTextPlan
import tri.promptfx.AiPlanTaskView
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.yaml

class SentimentAnalysisView: AiPlanTaskView("Sentiment Analysis",
    "Enter text to determine sentiment (or provide a numbered list of items)") {

    private val modeOptions = resources.yaml("resources/modes.yaml")["sentiment"] as List<String>
    private val sourceText = SimpleStringProperty("")
    private val mode = SimpleStringProperty(modeOptions[0])

    init {
        addInputTextArea(sourceText)
        parameters("Sentiment Mode") {
            field("Mode") {
                combobox(mode, modeOptions)
            }
        }
    }

    override fun plan() = completionEngine.instructTextPlan("sentiment-classify",
        instruct = mode.get(),
        userText = sourceText.get(),
        tokenLimit = 500)

}

class SentimentAnalysisPlugin : NavigableWorkspaceViewImpl<SentimentAnalysisView>("Text", "Sentiment Analysis", SentimentAnalysisView::class)