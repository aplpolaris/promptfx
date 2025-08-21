package tri.promptfx.`fun`

import javafx.beans.property.SimpleStringProperty
import tornadofx.text
import tornadofx.toolbar
import tri.promptfx.AiPlanTaskView
import tri.util.ui.NavigableWorkspaceViewImpl

/** Plugin for the [WikipediaView]. */
class WikipediaViewPlugin : NavigableWorkspaceViewImpl<WikipediaView>("Fun", "Wikipedia Q&A", type = WikipediaView::class)

/** View to answer questions using wikipedia. */
class WikipediaView: AiPlanTaskView("Wikipedia", "Enter a question to ask Wikipedia.") {

    private val input = SimpleStringProperty("")
    private val pageTitle = SimpleStringProperty("")

    init {
        addInputTextArea(input)
        input {
            toolbar {
                text("Wikipedia Page Source:")
            }
        }
        addInputTextArea(pageTitle)
    }

    override fun plan() = WikipediaAiTaskPlanner(completionEngine, pageTitle, input.get())

}