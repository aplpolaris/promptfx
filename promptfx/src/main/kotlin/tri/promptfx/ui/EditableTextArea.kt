/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package tri.promptfx.ui

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*

/** A word-wrapping [Text] that can be clicked to edit. */
class EditableTextArea(textProp: SimpleStringProperty) : Fragment() {
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
            prefWidth = 300.0
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
