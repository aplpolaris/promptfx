package tri.promptfx.tools

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.scene.control.*
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import javafx.stage.FileChooser
import tornadofx.*
import tri.ai.pips.AiPipelineResult
import tri.ai.text.chunks.*
import tri.promptfx.AiTaskView
import tri.promptfx.ui.TextChunkListView
import tri.promptfx.ui.TextChunkViewModel
import tri.promptfx.ui.asTextChunkViewModel
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.createListBinding

/** Plugin for the [TextLibraryView]. */
class TextManagerPlugin : NavigableWorkspaceViewImpl<TextLibraryView>("Tools", "Text Manager", TextLibraryView::class)

/** A view designed to help you manage collections of documents and text. */
class TextLibraryView : AiTaskView("Text Manager", "Manage collections of documents and text.") {

    private val libraryList = observableListOf<TextLibrary>()
    private lateinit var docList: ObservableList<TextDoc>
    private lateinit var chunkList: ObservableList<TextChunkViewModel>

    private lateinit var libraryListView: ListView<TextLibrary>
    private lateinit var librarySelection: ReadOnlyObjectProperty<TextLibrary>
    private lateinit var docListView: ListView<TextDoc>
    private lateinit var docSelection: ObservableList<TextDoc>
    private lateinit var chunkListView: TextChunkListView
    private lateinit var chunkSelection: ObservableList<TextChunkViewModel>

    init {
        input {
            val tb = toolbar { }

            libraryListView = listview(libraryList) {
                vgrow = Priority.ALWAYS
                selectionModel.selectionMode = SelectionMode.SINGLE
                librarySelection = selectionModel.selectedItemProperty()
                cellFormat {
                    graphic = Text(it.toString())
                }
            }
            docList = createListBinding(librarySelection) { it?.docs ?: listOf() }
            docListView = listview(docList) {
                vgrow = Priority.ALWAYS
                selectionModel.selectionMode = SelectionMode.MULTIPLE
                docSelection = selectionModel.selectedItems
                cellFormat {
                    graphic = Text(it.toString())
                }
            }
            chunkList = observableListOf()
            docSelection.onChange {
                chunkList.clear()
                chunkList.addAll(docSelection.flatMap { doc ->
                    doc.chunks.map { it.asTextChunkViewModel(doc.all) }
                })
            }

            chunkListView = TextChunkListView(chunkList, null, hostServices).apply {
                chunkSelection = root.selectionModel.selectedItems
            }
            add(chunkListView)

            // initialize toolbar after lists, since it uses dynamic properties configured above
            with (tb) {
                // generate chunks
                button("Create...", FontAwesomeIconView(FontAwesomeIcon.PLUS)) {
                    tooltip("Create a new text library.")
                    action { showTextLibraryWizard() }
                }
                // load a TextLibrary file
                button("Load...", FontAwesomeIconView(FontAwesomeIcon.UPLOAD)) {
                    tooltip("Load a text library from a JSON file.")
                    action {
                        chooseFile(
                            "Load Text Library",
                            filters = arrayOf(FileChooser.ExtensionFilter("JSON", "*.json")),
                            mode = FileChooserMode.Single
                        ).firstOrNull()?.let {
                            val lib = TextLibrary.loadFrom(it)
                            if (lib.metadata.id.isBlank())
                                lib.metadata.id = it.name
                            libraryList.add(lib)
                            libraryListView.selectionModel.select(lib)
                        }
                    }
                }
                // save a TextLibrary file
                button("Save...", FontAwesomeIconView(FontAwesomeIcon.DOWNLOAD)) {
                    tooltip("Save selected text library to a JSON file.")
                    enableWhen(librarySelection.isNotNull)
                    action {
                        librarySelection.value?.let { library ->
                            chooseFile(
                                "Save Text Library",
                                filters = arrayOf(FileChooser.ExtensionFilter("JSON", "*.json")),
                                mode = FileChooserMode.Save
                            ).firstOrNull()?.let {
                                TextLibrary.saveTo(library, it)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showTextLibraryWizard() {
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
                        libraryList.add(it)
                        libraryListView.selectionModel.select(it)
                        docListView.selectionModel.select(it.docs.first())
                    }
                }
                progressDialog.showAndWait()
            }
            openModal()
        }
    }

    override suspend fun processUserInput() = AiPipelineResult("", mapOf())
}