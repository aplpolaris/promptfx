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
package tri.promptfx.tools

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.binding.Bindings
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.embed.swing.SwingFXUtils
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.text.Text
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.embedding.cosineSimilarity
import tri.ai.pips.*
import tri.ai.pips.AiTask.Companion.task
import tri.ai.text.chunks.TextChunk
import tri.ai.text.chunks.TextDoc
import tri.ai.text.chunks.TextDocMetadata
import tri.ai.text.chunks.TextLibrary
import tri.ai.text.chunks.process.LocalFileManager.extractMetadata
import tri.ai.text.chunks.process.PdfMetadataGuesser
import tri.ai.text.chunks.process.TextDocEmbeddings.addEmbeddingInfo
import tri.ai.text.chunks.process.TextDocEmbeddings.getEmbeddingInfo
import tri.promptfx.*
import tri.promptfx.PromptFxConfig.Companion.DIR_KEY_TEXTLIB
import tri.promptfx.PromptFxConfig.Companion.FF_ALL
import tri.promptfx.PromptFxConfig.Companion.FF_JSON
import tri.promptfx.apps.ImageDescribeView
import tri.promptfx.docs.DocumentOpenInViewer
import tri.promptfx.ui.*
import tri.promptfx.ui.DocumentListView.Companion.icon
import tri.util.info
import tri.util.pdf.PdfUtils
import tri.util.ui.*
import java.awt.image.BufferedImage
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

/** Plugin for the [TextLibraryView]. */
class TextManagerPlugin : NavigableWorkspaceViewImpl<TextLibraryView>("Tools", "Text Manager", WorkspaceViewAffordance.COLLECTION_ONLY, TextLibraryView::class)

/** A view designed to help you manage collections of documents and text. */
class TextLibraryView : AiTaskView("Text Manager", "Manage collections of documents and text."), TextLibraryReceiver {

    val libraryList = observableListOf<TextLibraryInfo>()

    private lateinit var libraryListView: ListView<TextLibraryInfo>
    private lateinit var librarySelection: ReadOnlyObjectProperty<TextLibraryInfo>

    private lateinit var docList: ObservableList<TextDoc>
    private lateinit var docListView: ListView<TextDoc>
    private lateinit var docSelection: ObservableList<TextDoc>

    private val selectedDocImages = observableListOf<Image>()

    private val chunkFilter = SimpleObjectProperty<(TextChunk) -> Float>(null)
    private val isChunkFilterEnabled = SimpleBooleanProperty(false)
    private lateinit var chunkList: ObservableList<TextChunkViewModel>
    private lateinit var chunkListView: TextChunkListView
    private lateinit var chunkSelection: ObservableList<TextChunkViewModel>

    private val libraryId: ObservableValue<String>
    private val idChange = SimpleBooleanProperty(false)
    private val libraryInfo: ObservableValue<String>

    init {
        runButton.isVisible = false
        runButton.isManaged = false
        hideParameters()
    }

    //region INIT - INPUT PANEL

    init {
        input(5) {
            val tb = toolbar { }

            text("Document Collections")
            libraryListView = listview(libraryList) {
                vgrow = Priority.ALWAYS
                selectionModel.selectionMode = SelectionMode.SINGLE
                librarySelection = selectionModel.selectedItemProperty()
                cellFormat {
                    graphic = Text(it.library.toString())
                }
                lazyContextmenu {
                    buildsendcollectionmenu(this@TextLibraryView, librarySelection)
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
            docList = createListBinding(librarySelection) { it?.library?.docs ?: listOf() }
            docListView = listview(docList) {
                vgrow = Priority.ALWAYS
                selectionModel.selectionMode = SelectionMode.MULTIPLE
                docSelection = selectionModel.selectedItems
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
                            val firstPdf = docSelection.mapNotNull { it.browsable() }
                                .filter { it.path.substringAfterLast(".") == "pdf" }
                                .mapNotNull { it.file }
                                .firstOrNull { it.exists() }
                            if (firstPdf == null) {
                                alert(Alert.AlertType.ERROR, "No PDF file found for selected document(s).", owner = currentWindow)
                                return@action
                            } else {
                                runAsync {
                                    runBlocking {
                                        PdfMetadataGuesser.guessPdfMetadata(controller.completionEngine.value, firstPdf, 4)
                                    }
                                } ui {
                                    info<TextLibraryView>("Metadata guessed results: $it")
                                }
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
            chunkList = observableListOf()
            docSelection.onChange {
                refilterChunkList()
            }
            chunkFilter.onChange {
                refilterChunkList()
            }

            hbox(alignment = Pos.CENTER_LEFT) {
                val label = isChunkFilterEnabled.stringBinding {
                    if (it == true)
                        "Filtering/Ranking by Semantic Search"
                    else
                        "Text Chunks in Selected Document(s)"
                }
                text(label)
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
                                    runAsync {
                                        createSemanticFilter(it)
                                    } ui {
                                        chunkFilter.value = it
                                        isChunkFilterEnabled.set(true)
                                    }
                            }
                        else {
                            chunkFilter.value = null
                            isChunkFilterEnabled.set(false)
                        }
                    }
                }
            }
            chunkListView = TextChunkListView(chunkList, hostServices).apply {
                root.selectionModel.selectionMode = SelectionMode.MULTIPLE
                chunkSelection = root.selectionModel.selectedItems
                root.contextmenu {
                    val selectionString = Bindings.createStringBinding({ chunkSelection.joinToString("\n\n") { it.text } }, chunkSelection)
                    item("Find similar chunks") {
                        enableWhen(selectionString.isNotBlank())
                        action {
                            runAsync {
                                createSemanticFilter(selectionString.value)
                            } ui {
                                chunkFilter.value = it
                                isChunkFilterEnabled.set(true)
                            }
                        }
                    }
                    buildsendresultmenu(selectionString, workspace as PromptFxWorkspace)
                    separator()
                    item("Remove selected chunk(s) from document(s)") {
                        enableWhen(Bindings.isNotEmpty(chunkSelection))
                        action {
                            val selected = chunkSelection.toList()
                            val selectedChunks = selected.mapNotNull { (it as? TextChunkViewModelImpl)?.chunk }
                            docSelection.forEach { it.chunks.removeAll(selectedChunks) }
                            chunkList.removeAll(selected)
                        }
                    }

                }
            }
            add(chunkListView)

            // initialize toolbar after lists, since it uses dynamic properties configured above
            with (tb) {
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
                        textProperty().bind(Bindings.concat("Embeddings (", embeddingService.modelId, ")"))
                        tooltip("Calculate embedding vectors for all chunks in the currently selected library and embedding model.")
                        enableWhen { librarySelection.isNotNull }
                        action { executeEmbeddings() }
                    }
                }
            }
        }
    }

    //endregion

    //region INIT - VIEW MODEL VARS

    init {
        libraryId = librarySelection.stringBinding(idChange) { it?.library?.metadata?.id }
        libraryInfo = librarySelection.stringBinding { "${it?.library?.docs?.size ?: 0} documents" }
    }

    //endregion

    //region INIT - OUTPUT PANE

    init {
        with (outputPane) {
            clear()
            scrollpane {
                squeezebox(multiselect = true) {
                    vgrow = Priority.ALWAYS
                    fold("Details on Selected Collection", expanded = true) {
                        form {
                            fieldset("") {
                                visibleWhen { librarySelection.isNotNull }
                                managedWhen { librarySelection.isNotNull }
                                field("Id") { text(libraryId) }
                                field("Info") { text(libraryInfo) }
                            }
                        }
                    }
                    fold("Details on Selected Document", expanded = true) {
                        isFitToWidth = true
                        vbox(10) {
                            hgrow = Priority.ALWAYS
                            bindChildren(docSelection) { doc ->
                                val thumb =
                                    DocumentUtils.documentThumbnail(
                                        doc.browsable()!!,
                                        DocumentListView.DOC_THUMBNAIL_SIZE
                                    )
                                hbox(10) {
                                    if (thumb != null) {
                                        imageview(thumb) {
                                            fitWidth = 120.0
                                            isPreserveRatio = true
                                        }
                                    }
                                    form {
                                        hgrow = Priority.ALWAYS
                                        fieldset(doc.metadata.id.substringAfterLast("/")) {
                                            hgrow = Priority.ALWAYS
                                            fieldifnotblank("Id", doc.metadata.id)
                                            fieldifnotblank("Title", doc.metadata.title)
                                            fieldifnotblank("Author", doc.metadata.author)
                                            fieldifnotblank(
                                                "Date",
                                                doc.metadata.dateTime?.toString() ?: doc.metadata.date?.toString()
                                            )
                                            field("Path") {
                                                hyperlink(doc.metadata.path.toString()) {
                                                    action {
                                                        DocumentOpenInViewer(
                                                            doc.browsable()!!,
                                                            hostServices
                                                        ).open()
                                                    }
                                                }
                                                doc.metadata.path?.toString()
                                            }
                                            fieldifnotblank("Relative Path", doc.metadata.relativePath)
                                            fieldifnotblank(
                                                "Additional Properties",
                                                doc.metadata.properties.keys.joinToString(",")
                                            ) {
                                                tooltip(doc.metadata.properties.entries.joinToString("\n") { (k, v) -> "$k: $v" })
                                            }
                                            fieldifnotblank("Embeddings", doc.embeddingInfo())
                                        }
                                    }
                                }
                            }
                        }
                    }
                    fold("Images from Document", expanded = false) {
                        val thumbnailSize = SimpleDoubleProperty(128.0)
                        datagrid(selectedDocImages) {
                            vgrow = Priority.ALWAYS
                            prefWidth = 600.0
                            prefHeight = 600.0
                            cellWidthProperty.bind(thumbnailSize)
                            cellHeightProperty.bind(thumbnailSize)
                            cellCache {
                                imageview(it) {
                                    fitWidthProperty().bind(thumbnailSize)
                                    fitHeightProperty().bind(thumbnailSize)
                                    isPreserveRatio = true
                                    isPickOnBounds = true // so you can click anywhere on transparent images
                                    tooltip { graphic = imageview(it) }
                                    contextmenu {
                                        item("View full size").action { showImageDialog(image) }
                                        item("Copy to clipboard").action { copyToClipboard(image) }
                                        item("Send to Image Description View", graphic = FontAwesomeIcon.SEND.graphic) {
                                            action {
                                                val view = (workspace as PromptFxWorkspace).findTaskView("Image Description")
                                                (view as? ImageDescribeView)?.apply {
                                                    setImage(image)
                                                    workspace.dock(view)
                                                }
                                            }
                                        }
                                        item("Save to file...").action { saveToFile(image) }
                                    }
                                }
                            }
                        }
                    }
                    fold("Details on Selected Chunk(s)", expanded = true) {
                        vgrow = Priority.ALWAYS
                        form {
                            fieldset("") { }
                            vbox {
                                bindChildren(chunkSelection) { chunk ->
                                    fieldset("") {
                                        val text = chunk.text.trim()
                                        fieldifnotblank("Text", text) {
                                            contextmenu {
                                                item("Find similar chunks") {
                                                    action {
                                                        runAsync {
                                                            createSemanticFilter(text)
                                                        } ui {
                                                            chunkFilter.value = it
                                                            isChunkFilterEnabled.set(true)
                                                        }
                                                    }
                                                }
                                                buildsendresultmenu(text, workspace as PromptFxWorkspace)
                                            }
                                        }
                                        fieldifnotblank("Score", chunk.score?.toString())
                                        fieldifnotblank("Embeddings", chunk.embeddingsAvailable.joinToString(", "))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    //endregion

    //region INIT - LOAD DATA

    init {
        val filesToRestore = find<PromptFxConfig>().libraryFiles()
        filesToRestore.forEach { loadLibraryFrom(it) }

        // pull images from selected PDF's
        docSelection.onChange {
            selectedDocImages.clear()
            docSelection.forEach {
                runAsync {
                    val browsable = it.browsable()
                    val pdfFile = browsable?.file?.let { if (it.extension.lowercase() == "pdf") it else null }
                    if (pdfFile != null && pdfFile.exists()) {
                        PdfUtils.pdfPageInfo(pdfFile).flatMap { it.images }.mapNotNull { it.image }
                            .deduplicated()
                    } else {
                        listOf()
                    }
                } ui {
                    selectedDocImages.addAll(it.map { SwingFXUtils.toFXImage(it, null) })
                }
            }
        }
    }

    private fun List<BufferedImage>.deduplicated() =
        associateBy { it.hashCode() }.values.toList()

    //endregion

    //region UI HELPERS

    val REFILTER_LIST_MAX_COUNT = 20
    val MIN_CHUNK_SIMILARITY = 0.7

    /** Refilters the chunk list. */
    private fun refilterChunkList() {
        chunkList.clear()
        val filter = chunkFilter.value
        val chunksWithScores = docSelection.flatMap { doc -> doc.chunks.map { doc to it } }.let {
            if (filter == null)
                it.associateWith { null }
            else {
                it.associateWith { filter(it.second) }.entries
                    .sortedByDescending { it.value }
                    .take(REFILTER_LIST_MAX_COUNT)
                    .filter { it.value >= MIN_CHUNK_SIMILARITY }
                    .associate { it.key to it.value }
            }
        }
        chunkList.addAll(chunksWithScores.map {
            it.key.second.asTextChunkViewModel(it.key.first, embeddingService.modelId, score = it.value)
        })
    }

    /** Create semantic filtering, by returning the cosine similarity of a chunk to the given argument. */
    private fun createSemanticFilter(text: String): (TextChunk) -> Float {
        val model = embeddingService
        val embedding = runBlocking { model.calculateEmbedding(text) }
        return { chunk ->
            val chunkEmbedding = chunk.getEmbeddingInfo(model.modelId)
            if (chunkEmbedding == null) 0f else cosineSimilarity(embedding, chunkEmbedding).toFloat()
        }
    }

    /** Get embedding information, as list of calculated embedding models, within a [TextDoc]. */
    private fun TextDoc.embeddingInfo(): String {
        val models = chunks.flatMap { it.getEmbeddingInfo()?.keys ?: listOf() }.toSet()
        return if (models.isEmpty()) "No embeddings calculated."
        else models.joinToString(", ") { it }
    }

    /** Get embedding information, as list of calculated embedding models, within a [TextChunk]. */
    private fun TextChunk.embeddingInfo(): String {
        val models = getEmbeddingInfo()?.keys ?: listOf()
        return if (models.isEmpty()) "No embeddings calculated."
        else models.joinToString(", ") { it }
    }

    private fun EventTarget.fieldifnotblank(label: String, text: ObservableValue<String>) {
        field(label) {
            text(text) {
                wrappingWidth = 400.0
            }
            managedWhen(text.isNotBlank())
            visibleWhen(text.isNotBlank())
        }
    }

    private fun EventTarget.fieldifnotblank(label: String, text: String?, op: Field.() -> Unit = { }) {
        if (!text.isNullOrBlank())
            field(label) {
                labelContainer.alignment = Pos.TOP_LEFT
                text(text)
                op()
            }
    }

    //endregion

    //region USER ACTIONS

    override fun loadTextLibrary(library: TextLibraryInfo) {
        if (library !in libraryList) {
            libraryList.add(library)
            libraryListView.selectionModel.select(library)
        }
    }

    fun loadTextLibrary(library: TextLibrary) {
        val existing = libraryList.find { it.library.metadata.id == library.metadata.id }
        if (existing != null)
            libraryListView.selectionModel.select(existing)
        else {
            val info = TextLibraryInfo(library, null)
            libraryList.add(info)
            libraryListView.selectionModel.select(info)
        }
    }

    private fun renameSelectedCollection() {
        TextInputDialog(librarySelection.value.library.metadata.id).apply {
            initOwner(primaryStage)
            title = "Rename Collection"
            headerText = "Enter a new name for the collection."
            contentText = "Name:"
        }.showAndWait().ifPresent {
            librarySelection.value.library.metadata.id = it
            saveLibrary()
            libraryListView.refresh()
            idChange.set(!idChange.value) // force update of output pane
        }
    }

    private fun loadLibrary() {
        promptFxFileChooser(
            dirKey = DIR_KEY_TEXTLIB,
            title = "Load Text Library",
            filters = arrayOf(FF_JSON, FF_ALL),
            mode = FileChooserMode.Single
        ) {
            it.firstOrNull()?.let {
                loadLibraryFrom(it)
            }
        }
    }

    private fun loadLibraryFrom(file: File) {
        runAsync {
            val lib = TextLibrary.loadFrom(file)
            if (lib.metadata.id.isBlank())
                lib.metadata.id = file.name
            TextLibraryInfo(lib, file)
        } ui {
            libraryList.add(it)
            libraryListView.selectionModel.select(it)
        }
    }

    private fun saveLibrary() {
        librarySelection.value?.let { library ->
            promptFxFileChooser(
                dirKey = DIR_KEY_TEXTLIB,
                title = "Save Text Library",
                filters = arrayOf(FF_JSON, FF_ALL),
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
                        libraryList.add(libInfo)
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
            AiPipelineExecutor.execute(calculateEmbeddingsPlan().plan(), this@TextLibraryView.progress)
        }
    } ui {
        saveLibrary()
        chunkListView.refresh()
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

    //region TASKS

    override suspend fun processUserInput(): AiPipelineResult {
        return executeEmbeddings().value
    }

    private fun calculateEmbeddingsPlan(): AiPlanner {
        val service = embeddingService
        val result = mutableMapOf<TextChunk, List<Double>>()
        return listOf(librarySelection.value).flatMap { it.library.docs }.map { doc ->
            task("calculate-embeddings: " + doc.metadata.id) {
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

    //endregion
}

/** Track a library with where it was loaded from, null indicates not saved to a file. */
data class TextLibraryInfo(val library: TextLibrary, var file: File?)
