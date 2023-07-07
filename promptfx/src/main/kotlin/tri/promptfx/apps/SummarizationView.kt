package tri.promptfx.apps

import javafx.beans.property.SimpleStringProperty
import tornadofx.combobox
import tornadofx.field
import tri.ai.openai.templatePlan
import tri.ai.pips.AiPlanner
import tri.promptfx.AiPlanTaskView
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.yaml

/** A view that allows the user to summarize text. */
class SummarizationView: AiPlanTaskView("Summarization", "Enter text to summarize") {

    private val res = resources.yaml("resources/modes.yaml")
    private val audienceOptions = res["summarization-audience"] as Map<String, String>
    private val styleOptions = res["summarization-style"] as Map<String, String>
    private val outputOptions = res["summarization-format"] as Map<String, String>

    private val sourceText = SimpleStringProperty("")
    private val modeAudience = SimpleStringProperty(audienceOptions.keys.first())
    private val modeStyle = SimpleStringProperty(styleOptions.keys.first())
    private val modeOutput = SimpleStringProperty(outputOptions.keys.first())

    init {
        addInputTextArea(sourceText)
        parameters("Summarization Options") {
            field("Summarize for") {
                combobox(modeAudience, audienceOptions.keys.toList())
            }
            field("Style of") {
                combobox(modeStyle, styleOptions.keys.toList())
            }
            field("Shown as") {
                combobox(modeOutput, outputOptions.keys.toList())
            }
        }
    }

    override fun plan(): AiPlanner {
        val instruct = listOf(audienceOptions[modeAudience.value]!!,
            styleOptions[modeStyle.value]!!,
            outputOptions[modeOutput.value]!!)
            .filter { it.isNotBlank() }
            .joinToString(" and ")
        val fullInstruct = if (instruct.isBlank()) "" else "The result should be $instruct."
        return completionEngine.templatePlan("summarization",
            "instruct" to fullInstruct,
            "input" to sourceText.get(),
            tokenLimit = 500
        )
    }

}

class SummarizationPlugin : NavigableWorkspaceViewImpl<SummarizationView>("Text", "Summarization", SummarizationView::class)