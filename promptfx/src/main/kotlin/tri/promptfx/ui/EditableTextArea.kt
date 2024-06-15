package tri.promptfx.ui

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*

/** A text field that can be clicked to edit. */
class EditableTextField(textProp: SimpleStringProperty) : Fragment() {
    private val isEditing = SimpleBooleanProperty(false)

    override val root = hbox {
        textProp.onChange { tooltip(it) }
        val text = text(textProp) {
            wrappingWidth = 300.0
            managedProperty().bind(visibleProperty())
        }

        val textArea = textarea(textProp) {
            isVisible = false
            isWrapText = true
            prefColumnCount = 60
            managedProperty().bind(visibleProperty())
            focusedProperty().addListener { _, _, newValue ->
                if (!newValue)
                    isEditing.set(false)
            }
        }

        text.setOnMouseClicked { event ->
            if (event.clickCount == 2) {
                isEditing.set(true)
                textArea.isVisible = true
                textArea.requestFocus()
                textArea.selectAll()
            }
        }

        isEditing.addListener { _, _, newValue ->
            text.isVisible = !newValue
            textArea.isVisible = newValue
        }
    }
}