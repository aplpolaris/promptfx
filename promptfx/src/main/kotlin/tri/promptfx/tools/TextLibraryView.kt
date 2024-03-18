package tri.promptfx.tools

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.ListView
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import tornadofx.*
import tri.ai.pips.AiPipelineResult
import tri.ai.text.chunks.*
import tri.promptfx.AiTaskView
import tri.promptfx.ui.TextChunkListView
import tri.promptfx.ui.asTextChunkViewModel
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.createListBinding

/** Plugin for the [TextLibraryView]. */
class TextManagerPlugin : NavigableWorkspaceViewImpl<TextLibraryView>("Tools", "Text Manager", TextLibraryView::class)

/** A view designed to help you manage collections of documents and text. */
class TextLibraryView : AiTaskView("Text Manager", "Manage collections of documents and text.") {

    private val libraryList = observableListOf<TextLibrary>()
    private val librarySelection = SimpleObjectProperty<TextLibrary>()
    private val docList = createListBinding(librarySelection) { it?.docs ?: mutableListOf() }
    private val docSelection = SimpleObjectProperty<TextDoc>()
    private val chunkList = createListBinding(docSelection, { it?.chunks ?: mutableListOf() }, { doc, chunk -> chunk.asTextChunkViewModel(doc.all) })

    private lateinit var libraryListView: ListView<TextLibrary>
    private lateinit var docListView: ListView<TextDoc>
    private lateinit var chunkListView: TextChunkListView

    init {
        input {
            toolbar {
                // generate chunks
                button("New...", FontAwesomeIconView(FontAwesomeIcon.PLUS)) {
                    action {
                        find<TextChunkerWizard>().openModal()
                    }
                }
                // load a TextLibrary file
                button("Import...", FontAwesomeIconView(FontAwesomeIcon.DOWNLOAD)) {
                    action {
                        chooseFile(
                            "Select Text Library",
                            filters = arrayOf(),
                            mode = FileChooserMode.Single
                        ).firstOrNull()?.let {
                            val lib = TextLibrary.loadFrom(it)
                            lib.metadata.id = it.name
                            libraryList.add(lib)
                            libraryListView.selectionModel.select(lib)
                        }
                    }
                }
            }
            libraryListView = listview(libraryList) {
                vgrow = Priority.ALWAYS
                librarySelection.bind(selectionModel.selectedItemProperty())
                cellFormat {
                    graphic = Text(it.toString())
                }
            }
            docListView = listview(docList) {
                vgrow = Priority.ALWAYS
                docSelection.bind(selectionModel.selectedItemProperty())
                cellFormat {
                    graphic = Text(it.toString())
                }
            }
            chunkListView = TextChunkListView(chunkList, null, hostServices)
            add(chunkListView)
        }
    }

    override suspend fun processUserInput() = AiPipelineResult("", mapOf())
}