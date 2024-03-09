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
package tri.util.ui

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.Hyperlink
import javafx.scene.control.TextInputControl
import javafx.scene.input.TransferMode
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.Window
import tornadofx.chooseDirectory
import java.io.File

/** Configures a [TextInputControl] to accept dropped files and set its text to the content of the first file. */
fun TextInputControl.enableDroppingFileContent() {
    // enable dropping file content
    setOnDragOver { it.acceptTransferModes(*TransferMode.COPY_OR_MOVE) }
    setOnDragDropped {
        if (it.dragboard.hasFiles()) {
            textProperty().set(it.dragboard.files.first().readText())
        }
        it.isDropCompleted = true
        it.consume()
    }
}

fun TextFlow.plainText() = children.joinToString("") {
    (it as? Text)?.text ?:
    (it as? Hyperlink)?.text ?: ""
}

internal fun SimpleObjectProperty<File>.chooseFolder(owner: Window?) {
    chooseDirectory(
        title = "Select Document Folder",
        initialDirectory = value?.findDirectory(),
        owner = owner
    )?.let {
        set(it)
    }
}

fun icon(icon: FontAwesomeIcon) = FontAwesomeIconView(icon)

val FontAwesomeIcon.graphic
    get() = icon(this)

val FontAwesomeIconView.gray
    get() = apply {
        fill = Color.GRAY
    }

val FontAwesomeIconView.navy
    get() = apply {
        fill = Color.NAVY
    }
