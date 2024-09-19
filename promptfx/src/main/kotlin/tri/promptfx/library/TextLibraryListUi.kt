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
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.Priority
import tornadofx.*
import tri.promptfx.*
import tri.promptfx.tools.TextChunkerWizard
import tri.util.ui.bindSelectionBidirectional
import tri.util.ui.graphic

/** View for managing text collections and documents. */
class TextLibraryListUi : Fragment() {

    private val libraryModel by inject<TextLibraryViewModel>()
    private val progress: AiProgressView by inject()
    private val controller by inject<PromptFxController>()

    private val libraryList = libraryModel.libraryList
    private val librarySelection = libraryModel.librarySelection

    private lateinit var libraryListView: ListView<TextLibraryInfo>

    override val root = vbox {
        vgrow = Priority.SOMETIMES
        toolbar {
            text("Collections")
            spacer()
            // generate chunks
            button("Create...", FontAwesomeIconView(FontAwesomeIcon.PLUS)) {
                tooltip("Create a new text collection.")
                action { createLibraryWizard(libraryModel, replace = false, selectAllDocs = false) }
            }
            // load a TextLibrary file
            button("Load...", FontAwesomeIconView(FontAwesomeIcon.UPLOAD)) {
                tooltip("Load a text collection from a JSON file.")
                action { loadLibrary(libraryModel, replace = false, selectAllDocs = false) }
            }
            // save a TextLibrary file
            button("Save...", graphic = FontAwesomeIcon.DOWNLOAD.graphic) {
                tooltip("Save any modified collections to a JSON file.")
                enableWhen(librarySelection.isNotNull)
                action { saveLibrary() }
            }
            menubutton("Calculate/Extract", graphic = FontAwesomeIcon.COG.graphic) {
                enableWhen(librarySelection.isNotNull)
                tooltip("Options to extract or generate information for the selected collection.")
                item("Metadata", graphic = FontAwesomeIcon.INFO.graphic) {
                    tooltip("Extract metadata for all files in the selected collection. Metadata will be stored in a JSON file adjacent to the source file.")
                    enableWhen { librarySelection.isNotNull }
                    action { executeMetadataExtraction() }
                }
                item("Embeddings", graphic = FontAwesomeIcon.MAP_MARKER.graphic) {
                    textProperty().bind(Bindings.concat("Embeddings (", controller.embeddingService.value.modelId, ")"))
                    tooltip("Calculate embedding vectors for all chunks in the currently selected collection and embedding model.")
                    enableWhen { librarySelection.isNotNull }
                    action { executeEmbeddings() }
                }
            }
        }
        libraryListView = listview(libraryModel.libraryList) {
            vgrow = Priority.ALWAYS
            prefHeight = 100.0
            bindSelectionBidirectional(librarySelection)
            cellFormat {
                graphic = hbox(5, Pos.CENTER_LEFT) {
                    label(it.library.toString(), FontAwesomeIcon.BOOK.graphic)
                    text(libraryModel.savedStatusProperty(it)) {
                        style = "-fx-font-style: italic; -fx-text-fill: light-gray"
                    }
                }
            }
            lazyContextmenu {
                buildsendcollectionmenu(this@TextLibraryListUi, librarySelection)
                separator()
                item("Open collection file in system viewer") {
                    enableWhen(librarySelection.isNotNull)
                    action { librarySelection.value?.file?.let { hostServices.showDocument(it.absolutePath) } }
                }
                item("Open containing folder") {
                    enableWhen(librarySelection.isNotNull)
                    action { librarySelection.value?.file?.parentFile?.let { hostServices.showDocument(it.absolutePath) } }
                }
                separator()
                item("Rename collection") {
                    enableWhen(librarySelection.isNotNull)
                    action { renameSelectedCollection() }
                }
                separator()
                item("Remove collection from view") {
                    enableWhen(librarySelection.isNotNull)
                    action { librarySelection.value?.let { libraryList.remove(it) } }
                }
            }
            libraryModel.librariesModified.onChange { refresh() }
        }
    }

    //region USER ACTIONS

    private fun renameSelectedCollection() {
        val lib = libraryModel.librarySelection.value
        TextInputDialog(lib.library.metadata.id).apply {
            initOwner(primaryStage)
            title = "Rename Collection"
            headerText = "Enter a new name for the collection."
            contentText = "Name:"
        }.showAndWait().ifPresent {
            libraryModel.renameCollection(lib, it)
        }
    }

    private fun saveLibrary() {
        libraryModel.librarySelection.value?.let { library ->
            promptFxFileChooser(
                dirKey = PromptFxConfig.DIR_KEY_TEXTLIB,
                title = "Save Text Library",
                filters = arrayOf(PromptFxConfig.FF_JSON, PromptFxConfig.FF_ALL),
                mode = FileChooserMode.Save
            ) {
                it.firstOrNull()?.let {
                    libraryModel.saveLibrary(library, it)
                }
            }
        }
    }

    private fun executeEmbeddings() =
        libraryModel.calculateEmbeddingsTask(progress).ui {
            libraryModel.chunkListModel.refilter()
        }

    private fun executeMetadataExtraction() =
        libraryModel.extractMetadataTask().ui {
            alert(Alert.AlertType.INFORMATION, it, owner = currentWindow)
        }

    //endregion

}

//region USER ACTIONS

/** UI action to create a new text library. */
internal fun UIComponent.createLibraryWizard(libraryModel: TextLibraryViewModel, replace: Boolean, selectAllDocs: Boolean) {
    TextChunkerWizard().apply {
        onComplete {
            // show an indefinite progress indicator dialog while importing text in background
            val progressDialog = Dialog<ButtonType>().apply {
                graphic = ProgressIndicator(-1.0)
                title = "Creating Text Library"
                isResizable = false
                initOwner(this@createLibraryWizard.currentWindow)
                result = ButtonType.OK
            }
            runAsync {
                println("Creating library from user settings")
                model.finalLibrary {
                    runLater { progressDialog.contentText = it }
                }
            } ui {
                println("Created library: $it")
                progressDialog.close()
                if (it != null) {
                    val libInfo = TextLibraryInfo(it, null)
                    libraryModel.loadTextLibrary(libInfo, replace, selectAllDocs)
                }
            }
            progressDialog.showAndWait()
        }
        openModal()
    }
}

/** UI action to load a text library from a JSON file. */
internal fun UIComponent.loadLibrary(libraryModel: TextLibraryViewModel, replace: Boolean, selectAllDocs: Boolean) {
    promptFxFileChooser(
        dirKey = PromptFxConfig.DIR_KEY_TEXTLIB,
        title = "Load Text Library",
        filters = arrayOf(PromptFxConfig.FF_JSON, PromptFxConfig.FF_ALL),
        mode = FileChooserMode.Single
    ) {
        it.firstOrNull()?.let {
            libraryModel.loadLibraryFrom(it, replace, selectAllDocs)
        }
    }
}

//endregion

