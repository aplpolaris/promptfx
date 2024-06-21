package tri.promptfx.library

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.binding.Bindings
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.stage.Modality
import tornadofx.*
import tri.ai.text.chunks.TextDoc
import tri.promptfx.*
import tri.promptfx.docs.DocumentOpenInViewer
import tri.promptfx.tools.TextChunkerWizard
import tri.promptfx.ui.DocumentListView
import tri.promptfx.ui.DocumentListView.Companion.icon
import tri.util.ui.DocumentUtils
import tri.util.ui.bindSelectionBidirectional
import tri.util.ui.graphic

/** View for managing text collections and documents. */
class TextLibraryCollectionUi : Fragment() {

    private val model by inject<TextLibraryViewModel>()
    private val progress: AiProgressView by inject()
    private val controller by inject<PromptFxController>()

    private val libraryList = model.libraryList
    private val librarySelection = model.librarySelection
    private val docList = model.docList
    private val docSelection = model.docSelection

    private lateinit var libraryListView: ListView<TextLibraryInfo>
    private lateinit var docListView: ListView<TextDoc>

    override val root = vbox(5) {
        toolbar {
            // generate chunks
            button("Create...", FontAwesomeIconView(FontAwesomeIcon.PLUS)) {
                tooltip("Create a new text collection.")
                action { createLibraryWizard() }
            }
            // load a TextLibrary file
            button("Load...", FontAwesomeIconView(FontAwesomeIcon.UPLOAD)) {
                tooltip("Load a text collection from a JSON file.")
                action { loadLibrary() }
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

        text("Collections")
        libraryListView = listview(model.libraryList) {
            vgrow = Priority.ALWAYS
            bindSelectionBidirectional(librarySelection)
            cellFormat {
                graphic = hbox(5, Pos.CENTER_LEFT) {
                    label(it.library.toString(), FontAwesomeIcon.BOOK.graphic)
                    text(model.savedStatusProperty(it)) {
                        style = "-fx-font-style: italic; -fx-text-fill: light-gray"
                    }
                }
            }
            lazyContextmenu {
                buildsendcollectionmenu(this@TextLibraryCollectionUi, librarySelection)
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
            model.librariesModified.onChange { refresh() }
        }

        text("Documents in Selected Collection(s)")
        docListView = listview(docList) {
            vgrow = Priority.ALWAYS
            bindSelectionBidirectional(docSelection)
            cellFormat { doc ->
                val browsable = doc.browsable()!!
                graphic = hbox(5, Pos.CENTER_LEFT) {
                    hyperlink(browsable.shortNameWithoutExtension, graphic = browsable.icon()) {
                        val thumb = DocumentUtils.documentThumbnail(browsable, DocumentListView.DOC_THUMBNAIL_SIZE)
                        if (thumb != null) {
                            tooltip { graphic = ImageView(thumb) }
                        }
                        action { DocumentOpenInViewer(browsable, hostServices).open() }
                    }
                    text(model.savedStatusProperty(doc)) {
                        style = "-fx-font-style: italic; -fx-text-fill: light-gray"
                    }
                }
                lazyContextmenu {
                    item("Open metadata viewer...") {
                        isDisable = doc.pdfFile() == null
                        action { openMetadataViewer(doc) }
                    }
                    separator()
                    item("Remove selected document(s) from collection") {
                        enableWhen(Bindings.isNotEmpty(docSelection))
                        action {
                            model.removeSelectedDocuments()
                        }
                    }
                }
            }
        }
    }

    //region USER ACTIONS

    private fun renameSelectedCollection() {
        val lib = model.librarySelection.value
        TextInputDialog(lib.library.metadata.id).apply {
            initOwner(primaryStage)
            title = "Rename Collection"
            headerText = "Enter a new name for the collection."
            contentText = "Name:"
        }.showAndWait().ifPresent {
            model.renameCollection(lib, it)
        }
    }

    private fun loadLibrary() {
        promptFxFileChooser(
            dirKey = PromptFxConfig.DIR_KEY_TEXTLIB,
            title = "Load Text Library",
            filters = arrayOf(PromptFxConfig.FF_JSON, PromptFxConfig.FF_ALL),
            mode = FileChooserMode.Single
        ) {
            it.firstOrNull()?.let {
                model.loadLibraryFrom(it)
            }
        }
    }

    private fun saveLibrary() {
        model.librarySelection.value?.let { library ->
            promptFxFileChooser(
                dirKey = PromptFxConfig.DIR_KEY_TEXTLIB,
                title = "Save Text Library",
                filters = arrayOf(PromptFxConfig.FF_JSON, PromptFxConfig.FF_ALL),
                mode = FileChooserMode.Save
            ) {
                it.firstOrNull()?.let {
                    model.saveLibrary(library, it)
                }
            }
        }
    }

    private fun createLibraryWizard() {
        TextChunkerWizard().apply {
            onComplete {
                // show an indefinite progress indicator dialog while importing text in background
                val progressDialog = Dialog<ButtonType>().apply {
                    graphic = ProgressIndicator(-1.0)
                    title = "Creating Text Library"
                    isResizable = false
                    initOwner(currentWindow)
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
                        this@TextLibraryCollectionUi.model.libraryList.add(libInfo)
                        libraryListView.selectionModel.select(libInfo)
                        docListView.selectionModel.select(it.docs.first())
                    }
                }
                progressDialog.showAndWait()
            }
            openModal()
        }
    }

    private fun executeEmbeddings() =
        model.calculateEmbeddingsTask(progress).ui {
            model.refilterChunkList()
        }

    private fun executeMetadataExtraction() =
        model.extractMetadataTask().ui {
            alert(Alert.AlertType.INFORMATION, it, owner = currentWindow)
        }

    private fun openMetadataViewer(doc: TextDoc) {
        PdfViewerWithMetadataUi(doc) {
            model.updateMetadata(doc, it.editingValues(), isSelect = true)
        }.openModal(
            modality = Modality.NONE,
            block = false,
            resizable = true
        )
    }

    //endregion

}