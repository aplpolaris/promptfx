package tri.promptfx.tools

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.openai.templatePlan
import tri.ai.pips.aitask
import tri.ai.prompt.AiPrompt
import tri.promptfx.AiPlanTaskView
import tri.promptfx.CommonParameters
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.slider
import java.time.LocalDate

/** Plugin for the [PromptTemplateView]. */
class PromptTemplatePlugin : NavigableWorkspaceViewImpl<PromptTemplateView>("Tools", "Prompt Template", PromptTemplateView::class)

/** A view designed to help you test prompt templates. */
class PromptTemplateView : AiPlanTaskView("Prompt Template",
    "Enter a prompt template and a list of values to fill it in with.") {

    private val template = SimpleStringProperty("")
    private val fields = observableListOf<String>()
    private val fieldMap = mutableMapOf<String, String>()

    private val common = CommonParameters()
    private val maxTokens = SimpleIntegerProperty(500)

    init {
        template.onChange { updateTemplateInputs(it!!) }
    }

    init {
        input {
            spacing = 10.0
            paddingAll = 10.0
            text("Template:")
            textarea(template) {
                hgrow = Priority.ALWAYS
                prefRowCount = 10
                prefWidth = 0.0
            }
        }
        input {
            spacing = 10.0
            paddingAll = 10.0
            text("Inputs:")
            listview(fields) {
                cellFormat { field ->
                    graphic = hbox {
                        spacing = 10.0
                        alignment = Pos.CENTER
                        text(field)
                        val area = textarea(if (field == "today") LocalDate.now().toString() else "") {
                            hgrow = Priority.ALWAYS
                            prefRowCount = 1
                            textProperty().onChange { fieldMap[field] = it!! }
                        }
                        // add button to toggle expanding the text area
                        button("", FontAwesomeIconView(FontAwesomeIcon.EXPAND)) {
                            action { area.prefRowCount = if (area.prefRowCount == 1) 4 else 1 }
                        }
                        prefWidth = 0.0
                    }
                }
            }
        }
        parameters("Parameters") {
            with(common) {
                temperature()
                field("Max tokens") {
                    tooltip("Max # of tokens for combined query/response from the question answering engine")
                    slider(1..2000, maxTokens)
                    label(maxTokens)
                }
            }
        }
    }

    override fun plan() = aitask("text-completion") {
        AiPrompt(template.value).fill(fieldMap).let {
            completionEngine.complete(it, temperature = common.temp.value, tokens = maxTokens.value)
        }
    }.planner

    private fun updateTemplateInputs(template: String) {
        // extract {{{.}}} delimited fields from new value
        var templateText = template
        val nueFields = templateText.split("{{{").drop(1).map { it.substringBefore("}}}") }.toMutableList()
        nueFields.forEach { templateText = templateText.replace("{{{$it}}}", "") }
        nueFields.addAll(templateText.split("{{").drop(1).map { it.substringBefore("}}") })
        if (fields.toSet() != nueFields.toSet()) {
            fields.setAll(nueFields)
            fieldMap.clear()
        }
    }

}