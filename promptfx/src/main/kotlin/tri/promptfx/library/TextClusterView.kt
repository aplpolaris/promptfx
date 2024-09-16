package tri.promptfx.library

import tornadofx.*
import tri.ai.pips.AiPipelineResult
import tri.promptfx.AiTaskView
import tri.promptfx.TextLibraryReceiver
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance

/** Plugin for the [TextClusterView]. */
class TextClusterPlugin : NavigableWorkspaceViewImpl<TextClusterView>("Documents", "Text Clustering", WorkspaceViewAffordance.COLLECTION_ONLY, TextClusterView::class)

/** View designed to create clusters of selected text chunks. */
class TextClusterView : AiTaskView("Text Clustering", "Cluster documents and text."), TextLibraryReceiver {

    val model by inject<TextLibraryViewModel>()

    init {
        input {
            splitpane {
                add(TextLibraryCollectionListUi())
                add(TextLibraryDocumentListUi())
                add(TextLibraryFilterableChunkListView())
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