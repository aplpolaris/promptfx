package tri.promptfx.library

import javafx.geometry.Orientation
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.pips.AiPipelineResult
import tri.promptfx.AiTaskView
import tri.promptfx.TextLibraryReceiver
import tri.promptfx.ui.chunk.TextChunkListView
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance

/** Plugin for the [TextClusterView]. */
class TextClusterPlugin : NavigableWorkspaceViewImpl<TextClusterView>("Documents", "Text Clustering", WorkspaceViewAffordance.COLLECTION_ONLY, TextClusterView::class)

/** View designed to create clusters of selected text chunks. */
class TextClusterView : AiTaskView("Text Clustering", "Cluster documents and text."), TextLibraryReceiver {

    val viewScope = Scope(workspace)
    val model by inject<TextLibraryViewModel>(viewScope)

    init {
        input {
            splitpane(Orientation.VERTICAL) {
                vgrow = Priority.ALWAYS
                add(find<TextLibraryListUi>(viewScope))
                add(find<TextDocListUi>(viewScope))
                add(find<TextChunkListView>(viewScope))
            }
        }
    }

    override suspend fun processUserInput(): AiPipelineResult<*> {
        TODO("Not yet implemented")
    }

    override fun loadTextLibrary(library: TextLibraryInfo) {
        model.loadTextLibrary(library)
    }
}