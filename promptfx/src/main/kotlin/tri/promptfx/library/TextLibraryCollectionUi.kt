package tri.promptfx.library

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.binding.Bindings
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import javafx.stage.Modality
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.core.TextCompletion
import tri.ai.embedding.EmbeddingService
import tri.ai.pips.AiPipelineExecutor
import tri.ai.pips.AiPlanner
import tri.ai.pips.AiTask
import tri.ai.pips.aggregate
import tri.ai.text.chunks.TextChunk
import tri.ai.text.chunks.TextDoc
import tri.ai.text.chunks.TextDocMetadata
import tri.ai.text.chunks.TextLibrary
import tri.ai.text.chunks.process.LocalFileManager.extractMetadata
import tri.ai.text.chunks.process.PdfMetadataGuesser
import tri.ai.text.chunks.process.TextDocEmbeddings.addEmbeddingInfo
import tri.ai.text.chunks.process.TextDocEmbeddings.getEmbeddingInfo
import tri.promptfx.*
import tri.promptfx.docs.DocumentOpenInViewer
import tri.promptfx.docs.GuessedMetadataValidatorUi
import tri.promptfx.tools.TextChunkerWizard
import tri.promptfx.ui.DocumentListView
import tri.promptfx.ui.DocumentListView.Companion.icon
import tri.util.ui.DocumentUtils
import tri.util.ui.bindSelectionBidirectional
import tri.util.ui.graphic
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

/** View for managing text collections and documents. */
class TextLibraryCollectionUi : Fragment() {

    private val model by inject<TextLibraryViewModel>()
    private val progress: AiProgressView by inject()
    private val controller by inject<PromptFxController>()
    private val embeddingService: EmbeddingService by controller.embeddingService

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
                tooltip("Create a new text library.")
                action { createLibraryWizard() }
            }
            // load a TextLibrary file
            button("Load...", FontAwesomeIconView(FontAwesomeIcon.UPLOAD)) {
                tooltip("Load a text library from a JSON file.")
                action { loadLibrary() }
            }
            // save a TextLibrary file
            button("Save...", graphic = FontAwesomeIcon.DOWNLOAD.graphic) {
                tooltip("Save selected text library to a JSON file.")
                enableWhen(librarySelection.isNotNull)
                action { saveLibrary() }
            }
            menubutton("Calculate/Extract", graphic = FontAwesomeIcon.COG.graphic) {
                enableWhen(librarySelection.isNotNull)
                tooltip("Options to extract or generate information for the selected library.")
                item("Metadata", graphic = FontAwesomeIcon.INFO.graphic) {
                    tooltip("Extract metadata for all files in the selected library. Metadata will be stored in a JSON file adjacent to the source file.")
                    enableWhen { librarySelection.isNotNull }
                    action { executeMetadataExtraction() }
                }
                item("Embeddings", graphic = FontAwesomeIcon.MAP_MARKER.graphic) {
                    textProperty().bind(Bindings.concat("Embeddings (", controller.embeddingService.value.modelId, ")"))
                    tooltip("Calculate embedding vectors for all chunks in the currently selected library and embedding model.")
                    enableWhen { librarySelection.isNotNull }
                    action { executeEmbeddings() }
                }
            }
        }

        text("Document Collections")
        libraryListView = listview(model.libraryList) {
            vgrow = Priority.ALWAYS
            bindSelectionBidirectional(librarySelection)
            cellFormat {
                graphic = Text(it.library.toString())
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
                item("Remove selected collection from view") {
                    enableWhen(librarySelection.isNotNull)
                    action { librarySelection.value?.let { libraryList.remove(it) } }
                }
            }
        }

        text("Documents in Selected Collection(s)")
        docListView = listview(docList) {
            vgrow = Priority.ALWAYS
            bindSelectionBidirectional(docSelection)
            cellFormat {
                val browsable = it.browsable()!!
                graphic = hyperlink(browsable.shortNameWithoutExtension, graphic = browsable.icon()) {
                    val thumb = DocumentUtils.documentThumbnail(browsable, DocumentListView.DOC_THUMBNAIL_SIZE)
                    if (thumb != null) {
                        tooltip { graphic = ImageView(thumb) }
                    }
                    action { DocumentOpenInViewer(browsable, hostServices).open() }
                }
            }
            contextmenu {
                item("Guess metadata", graphic = FontAwesomeIcon.MAGIC.graphic) {
                    enableWhen(Bindings.isNotEmpty(docSelection))
                    action {
                        val firstDoc = docSelection.firstOrNull { it.pdfFile() != null }
                        val firstPdf = firstDoc?.pdfFile()
                        if (firstPdf == null) {
                            alert(
                                Alert.AlertType.ERROR,
                                "No PDF file found for selected document(s).",
                                owner = currentWindow
                            )
                            return@action
                        } else {
                            executeMetadataGuess(firstDoc, firstPdf, controller.completionEngine.value, )
                        }
                    }
                }
                separator()
                item("Remove selected document(s) from collection") {
                    enableWhen(Bindings.isNotEmpty(docSelection))
                    action {
                        val selected = docSelection.toList()
                        librarySelection.value?.library?.docs?.removeAll(selected)
                        docList.removeAll(selected)
                    }
                }
            }
        }
    }

    //region USER ACTIONS

    private fun renameSelectedCollection() {
        TextInputDialog(model.librarySelection.value.library.metadata.id).apply {
            initOwner(primaryStage)
            title = "Rename Collection"
            headerText = "Enter a new name for the collection."
            contentText = "Name:"
        }.showAndWait().ifPresent {
            model.librarySelection.value.library.metadata.id = it
            saveLibrary()
            libraryListView.refresh()
            model.libraryIdChange.set(!model.libraryIdChange.value) // force update of output pane
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
                    TextLibrary.saveTo(library.library, it)
                    library.file = it
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

    private fun executeEmbeddings() = runAsync {
        runBlocking {
            AiPipelineExecutor.execute(calculateEmbeddingsPlan().plan(), this@TextLibraryCollectionUi.progress)
        }
    } ui {
        saveLibrary()
        model.refilterChunkList()
    }

    private fun calculateEmbeddingsPlan(): AiPlanner {
        val service = embeddingService
        val result = mutableMapOf<TextChunk, List<Double>>()
        return listOf(librarySelection.value).flatMap { it.library.docs }.map { doc ->
            AiTask.task("calculate-embeddings: " + doc.metadata.id) {
                service.addEmbeddingInfo(doc)
                var count = 0
                doc.chunks.forEach {
                    val embed = it.getEmbeddingInfo(service.modelId)
                    if (embed != null) {
                        result[it] = embed
                        count++
                    }
                }
                "Calculated $count embeddings for ${doc.metadata.id}."
            }
        }.aggregate().task("summarize-results") {
            "Calculated ${result.size} total embeddings."
        }.planner
    }

    private fun executeMetadataExtraction() {
        var count = 0
        librarySelection.value.library.docs.forEach {
            val path = it.metadata.path
            if (path != null && File(path).exists()) {
                val md = File(path).extractMetadata()
                if (md.isNotEmpty()) {
                    count++
                    it.metadata.merge(md)
                }
            }
        }
        alert(Alert.AlertType.INFORMATION, "Extracted metadata from $count files.")
    }

    private fun executeMetadataGuess(doc: TextDoc, pdfFile: File, completion: TextCompletion) {
        runAsync {
            val result = runBlocking {
                PdfMetadataGuesser.guessPdfMetadata(completion, pdfFile, METADATA_GUESS_PAGE_LIMIT) {
                    this@TextLibraryCollectionUi.progress.taskStarted(it)
                }
            }
            this@TextLibraryCollectionUi.progress.taskCompleted()
            result
        } ui {
            val view = GuessedMetadataValidatorUi(it) {
                doc.metadata.merge(it.selectedValues())
                docSelection.setAll()
                docSelection.setAll(listOf(doc))
            }
            view.openModal(
                modality = Modality.NONE,
                block = false,
                resizable = true
            )
        }
    }

    private fun TextDocMetadata.merge(other: Map<String, Any>) {
        other.extract("title", "pdf.title", "doc.title") { title = it }
        other.extract("author", "pdf.author", "doc.author", "docx.author") { author = it }
        other.extractDate("date", "pdf.modificationDate", "pdf.creationDate", "doc.editTime", "docx.modified") { dateTime = it }
        properties.putAll(other)
    }

    private fun Map<String, Any>.extract(vararg keys: String, setter: (String) -> Unit) {
        keys.firstNotNullOfOrNull { get(it) }?.let { setter(it.toString()) }
    }

    private fun Map<String, Any>.extractDate(vararg keys: String, setter: (LocalDateTime) -> Unit) {
        keys.firstNotNullOfOrNull { get(it) }?.let {
            when (it) {
                is LocalDateTime -> setter(it)
                is LocalDate -> setter(it.atStartOfDay())
                else -> {
                    println("Could not parse date from $it, ${it.javaClass}")
                }
            }
        }
    }

    //endregion

    companion object {
        private val METADATA_GUESS_PAGE_LIMIT = 2
    }

}