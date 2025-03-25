/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
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
package tri.util.ui

import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.util.Callback
import tornadofx.*

/** Dialog for inputting a password. */
class PasswordDialog(defValue: String = "") : Dialog<String>() {
    private val grid = GridPane().apply {
        hgap = 10.0
        maxWidth = Double.MAX_VALUE
        alignment = Pos.CENTER_LEFT
    }
    private val label = Label("").apply {
        maxWidth = Double.MAX_VALUE
        maxHeight = Double.MAX_VALUE
        styleClass.add("content")
        isWrapText = true
        prefWidth = -1.0
    }
    internal val editor = PasswordField().apply {
        text = defValue
        maxWidth = Double.MAX_VALUE
        GridPane.setHgrow(this, Priority.ALWAYS)
        GridPane.setFillWidth(this, true)
    }

    init {
        label.textProperty().bind(dialogPane.contentTextProperty())
        dialogPane.contentTextProperty().onChange { updateGrid() }
        dialogPane.styleClass.add("text-input-dialog")
        dialogPane.buttonTypes.addAll(*arrayOf(ButtonType.OK, ButtonType.CANCEL))
        updateGrid()
        resultConverter = Callback {
            if (it?.buttonData == ButtonBar.ButtonData.OK_DONE) editor.text else null
        }
    }

    private fun updateGrid() {
        grid.children.clear()
        grid.add(this.label, 0, 0)
        grid.add(this.editor, 1, 0)
        dialogPane.content = this.grid
        Platform.runLater {
            editor.requestFocus()
        }
    }

}
