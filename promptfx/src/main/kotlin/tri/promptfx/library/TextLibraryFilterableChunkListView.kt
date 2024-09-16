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
package tri.promptfx.library

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.binding.Bindings
import javafx.scene.control.SelectionMode
import javafx.scene.control.TextInputDialog
import tornadofx.*
import tri.promptfx.PromptFxWorkspace
import tri.promptfx.buildsendresultmenu
import tri.promptfx.ui.TextChunkListView
import tri.util.ui.bindSelectionBidirectional

/** View for filtering and viewing text chunks. */
class TextLibraryFilterableChunkListView : Fragment() {

    val model by inject<TextLibraryViewModel>()

    private lateinit var chunkListView: TextChunkListView

    override val root = vbox {
        toolbar {
            text("Text Chunks")
            spacer()
            togglebutton(text = "", selectFirst = false) {
                graphic = FontAwesomeIconView(FontAwesomeIcon.FILTER)
                tooltip("Filter chunks by semantic text matching.")
                action {
                    if (isSelected)
                        TextInputDialog("").apply {
                            initOwner(primaryStage)
                            title = "Semantic Text for Chunk Search"
                            headerText = "Enter text to find similar text chunks."
                            contentText = "Semantic Text:"
                        }.showAndWait().ifPresent {
                            if (it.isNotBlank())
                                model.createSemanticFilter(it)
                        }
                    else {
                        model.chunkFilter.value = null
                        model.isChunkFilterEnabled.set(false)
                    }
                }
            }
        }
        chunkListView = TextChunkListView(model.chunkList).apply {
            root.selectionModel.selectionMode = SelectionMode.MULTIPLE
            root.bindSelectionBidirectional(model.chunkSelection)
            root.contextmenu {
                val selectionString =
                    Bindings.createStringBinding({ model.chunkSelection.joinToString("\n\n") { it.text } }, model.chunkSelection)
                item("Find similar chunks") {
                    enableWhen(selectionString.isNotBlank())
                    action { model.createSemanticFilter(selectionString.value) }
                }
                buildsendresultmenu(selectionString, workspace as PromptFxWorkspace)
                separator()
                item("Remove selected chunk(s) from document(s)") {
                    enableWhen(Bindings.isNotEmpty(model.chunkSelection))
                    action { model.removeSelectedChunks() }
                }

            }
        }
        add(chunkListView)
    }
}