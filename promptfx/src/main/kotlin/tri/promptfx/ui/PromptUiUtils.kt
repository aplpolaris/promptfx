package tri.promptfx.ui

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableStringValue
import javafx.event.EventTarget
import javafx.scene.layout.HBox
import tornadofx.*
import tri.promptfx.PromptFxWorkspace

/**
 * Adds a combobox for selecting a prompt, a text for seeing the prompt,
 * and an option to send the prompt to the template view.
 */
fun EventTarget.promptfield(
    fieldName: String = "Template",
    promptId: SimpleStringProperty,
    promptIdList: List<String>,
    promptText: ObservableStringValue,
    workspace: Workspace
) {
    val promptFieldVisible = SimpleBooleanProperty(false)
    field(fieldName) {
        (inputContainer as? HBox)?.spacing = 5.0
        combobox(promptId, promptIdList) {
            maxWidth = 200.0
        }
        togglebutton(text = "") {
            graphic = FontAwesomeIconView(FontAwesomeIcon.EYE)
            isSelected = false
            tooltip("Toggle visibility of the prompt text.")
            action { promptFieldVisible.set(!promptFieldVisible.value) }
        }
        button(text = "", graphic = FontAwesomeIconView(FontAwesomeIcon.SEND)) {
            tooltip("Copy this prompt to the Prompt Template view under Tools and open that view.")
            action { (workspace as PromptFxWorkspace).launchTemplateView(promptText.value) }
        }
    }
    field(null, forceLabelIndent = true) {
        text(promptText).apply {
            wrappingWidth = 300.0
            promptText.onChange { tooltip(it) }
        }
        visibleProperty().bindBidirectional(promptFieldVisible)
        managedProperty().bindBidirectional(promptFieldVisible)
    }
}