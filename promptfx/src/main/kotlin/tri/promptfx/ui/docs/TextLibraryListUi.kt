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
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.text.chunks.TextChunkRaw
import tri.ai.text.chunks.TextDoc
import tri.ai.text.chunks.TextDocEmbeddings.addEmbeddingInfo
import tri.ai.text.chunks.TextLibrary
import tri.promptfx.*
import tri.promptfx.docs.TextLibraryInfo
import tri.util.info
import tri.util.io.LocalFileManager.extractMetadata
import tri.util.ui.bindSelectionBidirectional
import tri.util.ui.graphic
import java.io.File

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
            menubutton("Create", FontAwesomeIcon.PLUS_CIRCLE.graphic) {
                tooltip("Create a new text collection.")
                item("Using Wizard...", graphic = FontAwesomeIcon.MAGIC.graphic) {
                    tooltip("Create a new text collection from a single text file.")
                    action { createLibraryWizard(libraryModel, replace = false, selectAllDocs = true) }
                }
                item("From Lines of Text...", graphic = FontAwesomeIcon.FILE_TEXT_ALT.graphic) {
                    tooltip("Create a new text collection from lines of text.")
                    action { createLibraryLines(libraryModel, replace = false, selectAllDocs = false) }
                }
            }
            // load a TextLibrary file
            button("Open...", FontAwesomeIcon.FOLDER_OPEN.graphic) {
                tooltip("Open an existing text collection from a JSON file.")
                action { loadLibrary(libraryModel, replace = false, selectAllDocs = false) }
            }
            // save a TextLibrary file
            button("Save...", graphic = FontAwesomeIcon.SAVE.graphic) {
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
                    textProperty().bind(Bindings.concat("Embeddings (", controller.embeddingStrategy.value.modelId, ")"))
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

/** UI action to create a library from lines user pastes into a dialog. */
fun UIComponent.createLibraryLines(libraryModel: TextLibraryViewModel, replace: Boolean, selectAllDocs: Boolean) {
    val dialog = find<PasteTextLibraryDialog>()
    dialog.openModal(block = true)
    if (dialog.lines.isNotEmpty()) {
        val library = TextLibrary("User Input").apply {
            docs.add(TextDoc("User Input").apply {
                chunks.addAll(dialog.lines.map { TextChunkRaw(it) })
            })
        }
        libraryModel.loadTextLibrary(TextLibraryInfo(library, null), replace, selectAllDocs)
    }
}

/** UI for pasting text into a dialog. */
class PasteTextLibraryDialog: Fragment("Paste Text Library") {
    val userText = SimpleStringProperty()
    val lines = mutableListOf<String>()

    override val root = vbox {
        paddingAll = 10.0
        text("Paste lines of text into the box below to create a new collection of text chunks.")
        textarea(userText) {
            prefRowCount = 30
            prefColumnCount = 80
            promptText = "Paste lines of text here..."
        }
        buttonbar {
            paddingTop = 10
            button("OK", ButtonBar.ButtonData.OK_DONE) {
                action {
                    lines.addAll(userText.value.lines().filter { it.isNotBlank() }.distinct())
                    close()
                }
            }
            button("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE) {
                action { close() }
            }
        }
    }
}

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
                info<TextLibraryListUi>("Creating library based on user selection")
                
                // Step 1: Create the library
                val library = model.finalLibrary {
                    runLater { progressDialog.contentText = "Creating library: $it" }
                }
                
                if (library != null && model.libraryFile.value != null) {
                    val targetFile = model.libraryFile.value!!
                    
                    // Step 2: Save the library to the specified file
                    runLater { progressDialog.contentText = "Saving library to ${targetFile.name}..." }
                    TextLibrary.saveTo(library, targetFile)
                    val libInfo = TextLibraryInfo(library, targetFile)
                    
                    // Step 3: Optional metadata extraction
                    if (model.extractMetadata.value) {
                        runLater { progressDialog.contentText = "Extracting metadata..." }
                        var metadataCount = 0
                        library.docs.forEach { doc ->
                            val path = doc.metadata.path
                            if (path != null && File(path).exists()) {
                                val metadata = File(path).extractMetadata()
                                if (metadata.isNotEmpty()) {
                                    doc.metadata.replaceAll(metadata)
                                    metadataCount++
                                }
                            }
                        }
                        if (metadataCount > 0) {
                            TextLibrary.saveTo(library, targetFile) // Re-save with metadata
                        }
                        info<TextLibraryListUi>("Extracted metadata from $metadataCount documents")
                    }
                    
                    // Step 4: Optional embedding generation
                    if (model.generateEmbeddings.value && model.embeddingModel.value != null) {
                        runLater { progressDialog.contentText = "Generating embeddings..." }
                        val embeddingModel = model.embeddingModel.value!!
                        var documentCount = 0
                        library.docs.forEach { doc ->
                            if (doc.chunks.isNotEmpty()) {
                                try {
                                    runLater { progressDialog.contentText = "Generating embeddings for ${doc.metadata.title}..." }
                                    embeddingModel.addEmbeddingInfo(doc)
                                    documentCount++
                                } catch (e: Exception) {
                                    info<TextLibraryListUi>("Failed to generate embeddings for document ${doc.metadata.title}: ${e.message}")
                                }
                            }
                        }
                        if (documentCount > 0) {
                            TextLibrary.saveTo(library, targetFile) // Re-save with embeddings
                        }
                        info<TextLibraryListUi>("Generated embeddings for $documentCount documents")
                    }
                    
                    libInfo
                } else {
                    null
                }
            } ui { libInfo ->
                progressDialog.close()
                if (libInfo != null) {
                    info<TextLibraryListUi>("Created and saved library: ${libInfo.library}")
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

