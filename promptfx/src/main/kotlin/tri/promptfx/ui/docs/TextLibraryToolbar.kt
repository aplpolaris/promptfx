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
package tri.promptfx.ui.docs

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.promptfx.PromptFxWorkspace
import tri.util.ui.graphic

/** A toolbar for users to create or select an existing library. */
class TextLibraryToolbar : Fragment() {

    private val libraryModel by inject<TextLibraryViewModel>()
    private val separateLibraryLabel: Boolean by param(false)
    val titleText = SimpleStringProperty("Collection")
    private val libraryName = libraryModel.librarySelection.stringBinding {
        it?.library?.metadata?.id ?: "No collection selected"
    }

    override val root = vbox {
        toolbar {
            text(titleText)
            spacer()
            if (!separateLibraryLabel) {
                text(libraryName) {
                    style = "-fx-font-style: italic;"
                }
            }
            // open wizard to create a new TextLibrary
            button("", FontAwesomeIcon.PLUS_CIRCLE.graphic) {
                tooltip("Create a new text collection.")
                action { createLibraryWizard(libraryModel, replace = true, selectAllDocs = true) }
            }
            // load a TextLibrary file
            button("", FontAwesomeIcon.FOLDER_OPEN.graphic) {
                tooltip("Load a text collection from a JSON file.")
                action { loadLibrary(libraryModel, replace = true, selectAllDocs = true) }
            }
            // send the library to TextManager view
            button("", FontAwesomeIcon.SEND.graphic) {
                enableWhen(libraryModel.librarySelection.isNotNull)
                tooltip("Open the current library in the Text Manager view")
                action {
                    find<PromptFxWorkspace>().launchTextManagerView(libraryModel.librarySelection.get().library)
                }
            }
        }
        if (separateLibraryLabel) {
            text(libraryName) {
                style = "-fx-font-style: italic;"
            }
        }
    }

}
