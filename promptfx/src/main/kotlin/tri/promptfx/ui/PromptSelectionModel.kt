package tri.promptfx.ui

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.prompt.AiPromptLibrary

/** Model for a prompt id and lookup result in prompt table. */
class PromptSelectionModel(_id: String) {
    val id = SimpleStringProperty(_id)
    val prompt = SimpleObjectProperty(AiPromptLibrary.lookupPrompt(id.value))
    val text = SimpleStringProperty(prompt.value!!.template)

    init {
        id.onChange { prompt.set(AiPromptLibrary.lookupPrompt(it!!)) }
        prompt.onChange { text.set(it!!.template) }
    }
}