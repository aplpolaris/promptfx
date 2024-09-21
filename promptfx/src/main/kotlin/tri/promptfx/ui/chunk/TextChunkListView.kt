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
package tri.promptfx.ui.chunk

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.ListView
import javafx.scene.control.SelectionMode
import javafx.scene.layout.Priority
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.openai.jsonMapper
import tri.promptfx.PromptFxConfig.Companion.DIR_KEY_TEXTLIB
import tri.promptfx.PromptFxConfig.Companion.FF_ALL
import tri.promptfx.PromptFxConfig.Companion.FF_JSON
import tri.promptfx.PromptFxWorkspace
import tri.promptfx.buildsendresultmenu
import tri.promptfx.ui.docs.TextLibraryViewModel
import tri.promptfx.promptFxFileChooser
import tri.util.ui.bindSelectionBidirectional

/**
 * View showing a list of text chunks, each of which may be optionally paired with additional metadata.
 * In addition to a list of chunks, this view has an optional filter UI (for filtering by find, regex, embedding, etc.),
 * an export option, and context menu options.
 */
class TextChunkListView(_label: String? = "Text Chunks"): Fragment() {

    /** Name for the view. */
    val label = SimpleStringProperty(_label)

    // TODO - avoid injecting library here if possible? (needed to remove chunks from parent docs)
    private val libraryModel: TextLibraryViewModel by inject()

    private val filterToggleVisible = SimpleBooleanProperty(true)
    private val filterUiVisible = SimpleBooleanProperty(false)
    internal val model by inject<TextChunkListModel>()
    private val chunkSelection = model.chunkSelection

    private lateinit var chunkListView: ListView<TextChunkViewModel>

    override val root = vbox {
        vgrow = Priority.ALWAYS
        toolbar {
            text(label)
            spacer()
            togglebutton(text = "", selectFirst = false) {
                visibleWhen(filterToggleVisible)
                managedWhen(filterToggleVisible)
                graphic = FontAwesomeIconView(FontAwesomeIcon.FILTER)
                tooltip("Show/hide filter UI")
                selectedProperty().bindBidirectional(filterUiVisible)
            }
            button("", FontAwesomeIconView(FontAwesomeIcon.DOWNLOAD)) {
                disableWhen(model.chunkList.sizeProperty.isEqualTo(0))
                action { exportFilteredChunkList() }
            }
        }
        add(TextChunkFilterUi().root.apply {
            visibleWhen(filterUiVisible)
            managedWhen(filterUiVisible)
        })
        chunkListView = listview(model.filteredChunkList) {
            vgrow = Priority.ALWAYS
            prefHeight = 300.0
            cellFormat {
                graphic = TextChunkView(it, hostServices)
            }
            selectionModel.selectionMode = SelectionMode.MULTIPLE
            bindSelectionBidirectional(chunkSelection)
            lazyContextmenu {
                val selectionString = chunkSelection.joinToString("\n\n") { it.text }
                item("Find similar chunks") {
                    isDisable = selectionString.isBlank()
                    action {
                        model.applyEmbeddingFilter(selectionString)
                    }
                }
                buildsendresultmenu(selectionString, workspace as PromptFxWorkspace)
                separator()
                item("Remove selected chunk(s) from document(s)") {
                    enableWhen(Bindings.isNotEmpty(chunkSelection))
                    action { libraryModel.removeSelectedChunks() }
                }

            }
        }
        add(chunkListView)
    }

    private fun exportFilteredChunkList() {
        promptFxFileChooser(
            dirKey = DIR_KEY_TEXTLIB,
            title = "Export Document Snippets as JSON",
            filters = arrayOf(FF_JSON, FF_ALL),
            mode = FileChooserMode.Save
        ) {
            if (it.isNotEmpty()) {
                runAsync {
                    runBlocking {
                        // TODO - can we export library with scored chunks instead of this object??
                        jsonMapper.writerWithDefaultPrettyPrinter()
                            .writeValue(it.first(), model.filteredChunkList)
                    }
                }
            }
        }
    }

//    private fun exportDocumentSnippets() {
//        promptFxFileChooser(
//            dirKey = DIR_KEY_TEXTLIB,
//            title = "Export Document Snippets as JSON",
//            filters = arrayOf(FF_JSON, FF_ALL),
//            mode = FileChooserMode.Save
//        ) {
//            if (it.isNotEmpty()) {
//                runAsync {
//                    runBlocking {
//                        jsonMapper
//                            .writerWithDefaultPrettyPrinter()
//                            .writeValue(it.first(), planner.lastResult)
//                    }
//                }
//            }
//        }
//    }
}