package tri.promptfx.tools

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.binding.Bindings
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.scene.control.*
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.pips.*
import tri.ai.pips.AiTask.Companion.task
import tri.ai.text.chunks.TextChunk
import tri.ai.text.chunks.TextDoc
import tri.ai.text.chunks.TextLibrary
import tri.ai.text.chunks.process.EmbeddingPrecision
import tri.ai.text.chunks.process.TextDocEmbeddings.addEmbeddingInfo
import tri.ai.text.chunks.process.TextDocEmbeddings.getEmbeddingInfo
import tri.promptfx.AiTaskView
import tri.promptfx.PromptFx
import tri.promptfx.PromptFxConfig
import tri.promptfx.PromptFxConfig.Companion.DIR_KEY_TEXTLIB
import tri.promptfx.PromptFxConfig.Companion.FF_ALL
import tri.promptfx.PromptFxConfig.Companion.FF_JSON
import tri.promptfx.promptFxFileChooser
import tri.promptfx.ui.TextChunkListView
import tri.promptfx.ui.TextChunkViewModel
import tri.promptfx.ui.TextChunkViewModelImpl
import tri.promptfx.ui.asTextChunkViewModel
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.createListBinding
import java.io.File

/** Plugin for the [TextLibraryView]. */
class TextManagerPlugin : NavigableWorkspaceViewImpl<TextLibraryView>("Tools", "Text Manager", TextLibraryView::class)

/** A view designed to help you manage collections of documents and text. */
class TextLibraryView : AiTaskView("Text Manager", "Manage collections of documents and text.") {

    val libraryList = observableListOf<TextLibraryInfo>()
    private lateinit var docList: ObservableList<TextDoc>
    private lateinit var chunkList: ObservableList<TextChunkViewModel>

    private lateinit var libraryListView: ListView<TextLibraryInfo>
    private lateinit var librarySelection: ReadOnlyObjectProperty<TextLibraryInfo>
    private lateinit var docListView: ListView<TextDoc>
    private lateinit var docSelection: ObservableList<TextDoc>
    private lateinit var chunkListView: TextChunkListView
    private lateinit var chunkSelection: ObservableList<TextChunkViewModel>

    private val libraryId: ObservableValue<String>
    private val libraryInfo: ObservableValue<String>
    private val docsId: ObservableValue<String>
    private val docsTitle: ObservableValue<String>
    private val docsAuthor: ObservableValue<String>
    private val docsDate: ObservableValue<String>
    private val docsPath: ObservableValue<String>
    private val docsRelativePath: ObservableValue<String>
    private val chunkType: ObservableValue<String>
    private val chunksText: ObservableValue<String>
    private val chunksScore: ObservableValue<String>
    private val chunksEmbedding: ObservableValue<String>

    init {
        runButton.isVisible = false
        runButton.isManaged = false
    }

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
                contextmenu {
                    item("Remove Selected Library") {
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
                    graphic = Text(it.toString())
                }
                contextmenu {
                    item("Remove Selected Document(s)") {
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
                chunkList.clear()
                chunkList.addAll(docSelection.flatMap { doc ->
                    doc.chunks.map { it.asTextChunkViewModel(doc.all, embeddingService.modelId) }
                })
            }

            text("Text Chunks in Selected Document(s)")
            chunkListView = TextChunkListView(chunkList, null, hostServices).apply {
                root.selectionModel.selectionMode = SelectionMode.MULTIPLE
                chunkSelection = root.selectionModel.selectedItems
                root.contextmenu {
                    item("Remove Selected Chunk(s)") {
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
                button("Save...", FontAwesomeIconView(FontAwesomeIcon.DOWNLOAD)) {
                    tooltip("Save selected text library to a JSON file.")
                    enableWhen(librarySelection.isNotNull)
                    action { saveLibrary() }
                }
                // calculate embeddings
                button("Calculate Embeddings", FontAwesomeIconView(FontAwesomeIcon.MAP_MARKER)) {
                    tooltip("Calculate embedding vectors for all chunks in the currently selected library.")
                    enableWhen { librarySelection.isNotNull }
                    action { executeEmbeddings() }
                }
            }
        }
    }

    init {
        libraryId = librarySelection.stringBinding { it?.library?.metadata?.id }
        libraryInfo = librarySelection.stringBinding { "${it?.library?.docs?.size ?: 0} documents" }
        docsId = Bindings.createStringBinding({ docSelection.joinToString("\n") { it.metadata.id }.trim() }, docSelection)
        docsTitle = Bindings.createStringBinding({ docSelection.joinToString("\n") { it.metadata.title ?: "" }.trim() }, docSelection)
        docsAuthor = Bindings.createStringBinding({ docSelection.joinToString("\n") { it.metadata.author ?: "" }.trim() }, docSelection)
        docsDate = Bindings.createStringBinding({ docSelection.joinToString("\n") { it.metadata.date?.toString() ?: "" }.trim() }, docSelection)
        docsPath = Bindings.createStringBinding({ docSelection.joinToString("\n") { it.metadata.path ?: "" }.trim() }, docSelection)
        docsRelativePath = Bindings.createStringBinding({ docSelection.joinToString("\n") { it.metadata.relativePath ?: "" }.trim() }, docSelection)
        chunkType = Bindings.createStringBinding({ chunkSelection.joinToString("\n") { "" } }, chunkSelection) // TODO
        chunksText = Bindings.createStringBinding({ chunkSelection.joinToString("\n") { it.text }.trim() }, chunkSelection)
        chunksScore = Bindings.createStringBinding({ chunkSelection.joinToString("\n") { it.score?.toString() ?: "" }.trim() }, chunkSelection)
        chunksEmbedding = Bindings.createStringBinding({ chunkSelection.count {
            it.embedding != null
        }.let { if (it > 0) "$it Embeddings Calculated" else "" } }, chunkSelection)
    }

    init {
        with (outputPane) {
            clear()
            scrollpane {
                form {
                    fieldset("Selected Library") {
                        visibleWhen { librarySelection.isNotNull }
                        managedWhen { librarySelection.isNotNull }
                        field("Id") { text(libraryId) }
                        field("Info") { text(libraryInfo) }
                    }
                    fieldset("Selected Document(s)") {
                        visibleWhen { Bindings.isNotEmpty(docSelection) }
                        managedWhen { Bindings.isNotEmpty(docSelection) }
                        fieldifnotblank("Id", docsId)
                        fieldifnotblank("Title", docsTitle)
                        fieldifnotblank("Author", docsAuthor)
                        fieldifnotblank("Date", docsDate)
                        fieldifnotblank("Path", docsPath)
                        fieldifnotblank("Relative Path", docsRelativePath)
                    }
                    fieldset("Selected Text Chunk(s)") {
                        visibleWhen { Bindings.isNotEmpty(chunkSelection) }
                        managedWhen { Bindings.isNotEmpty(chunkSelection) }
                        fieldifnotblank("Type", chunkType)
                        fieldifnotblank("Text", chunksText)
                        fieldifnotblank("Score", chunksScore)
                        fieldifnotblank("Embedding", chunksEmbedding)
                    }
                }
            }
        }
    }

    init {
        val filesToRestore = find<PromptFxConfig>().libraryFiles()
        filesToRestore.forEach { loadLibraryFrom(it) }
    }

    //region UI HELPERS

    private fun EventTarget.fieldifnotblank(label: String, text: ObservableValue<String>) {
        field(label) {
            text(text) {
                wrappingWidth = 400.0
            }
            managedWhen(text.isNotBlank())
            visibleWhen(text.isNotBlank())
        }
    }

    //endregion

    //region USER ACTIONS

    private fun loadLibrary() {
        promptFxFileChooser(
            dirKey = DIR_KEY_TEXTLIB,
            title = "Load Text Library",
            filters = arrayOf(FF_JSON, FF_ALL),
            mode = FileChooserMode.Single
        ) {
            it.firstOrNull()?.let {
                val libInfo = loadLibraryFrom(it)
                libraryListView.selectionModel.select(libInfo)
            }
        }
    }

    private fun loadLibraryFrom(file: File): TextLibraryInfo {
        val lib = TextLibrary.loadFrom(file)
        if (lib.metadata.id.isBlank())
            lib.metadata.id = file.name
        val libInfo = TextLibraryInfo(lib, file)
        libraryList.add(libInfo)
        return libInfo
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
        chunkListView.refresh()
    }

    //endregion

    //region TASKS

    override suspend fun processUserInput(): AiPipelineResult {
        return executeEmbeddings().value
    }

    private fun calculateEmbeddingsPlan(): AiPlanner {
        val service = embeddingService
        val result = mutableMapOf<TextChunk, List<Double>>()
        return libraryList.flatMap { it.library.docs }.map { doc ->
            task("calculate-embeddings: " + doc.metadata.id) {
                service.addEmbeddingInfo(doc, EmbeddingPrecision.FIRST_FOUR)
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