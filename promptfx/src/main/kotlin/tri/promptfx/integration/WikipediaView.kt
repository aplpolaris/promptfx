package tri.promptfx.integration

import javafx.beans.property.SimpleStringProperty
import javafx.scene.text.Text
import tornadofx.*
import tri.promptfx.AiPlanTaskView
import tri.util.ui.NavigableWorkspaceViewImpl

class WikipediaView: AiPlanTaskView("Wikipedia", "Enter a question to ask Wikipedia.") {

    private val input = SimpleStringProperty("")
    private val pageTitle = SimpleStringProperty("")

    init {
        addInputTextArea(input)
        inputPane.add(Text("Wikipedia Page Source:"))
        inputPane.add(textarea(pageTitle) {
            isWrapText = true
        })
    }

    override fun plan() = WikipediaAiTaskPlanner(completionEngine, pageTitle, input.get())

}

class WikipediaViewPlugin : NavigableWorkspaceViewImpl<WikipediaView>("Integrations", "Wikipedia", WikipediaView::class)