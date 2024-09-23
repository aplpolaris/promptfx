package tri.promptfx.library

import javafx.geometry.Orientation
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.pips.AiPipelineExecutor
import tri.ai.pips.AiPipelineResult
import tri.ai.pips.AiTask
import tri.ai.prompt.trace.AiPromptInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiPromptTraceSupport
import tri.promptfx.AiTaskView
import tri.promptfx.TextLibraryReceiver
import tri.promptfx.library.TextClustering.generateClusterHierarchy
import tri.promptfx.ui.FormattedPromptTraceResult
import tri.promptfx.ui.FormattedText
import tri.promptfx.ui.FormattedTextNode
import tri.promptfx.ui.chunk.TextChunkListView
import tri.promptfx.ui.docs.TextDocListUi
import tri.promptfx.ui.docs.TextLibraryListUi
import tri.promptfx.ui.docs.TextLibraryViewModel
import tri.util.ml.AffinityClusterService
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance
import tri.util.ui.starship.AiPromptExecutor

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
        val monitorTask = AiTask.task("clustering") { }
        val task = AiTask.task("clustering") {
            // calculate embeddings
            val task2 = model.calculateEmbeddingsTask(progress)
            task2.get()
            val chunks = model.chunkListModel.filteredChunkList.toList()

            // generate clusters
            val hierarchy = AffinityClusterService().generateClusterHierarchy(
                chunks,
                itemType = "text snippets",
                categories = listOf("chunk") ,
                sampleTheme = "These snippets appear to discuss animals or pets.",
                completionEngine = controller.completionEngine.value,
                embeddingService = controller.embeddingService.value,
                attempts = 3,
                progressPercent = { progress.taskUpdate(monitorTask, it) }
            )

            // pretty print clusters
            FormattedText(hierarchy.map { printCluster(it, "\n") }.flatten())
        }
        return AiPipelineExecutor.execute(listOf(task), progress)
    }

    private fun printCluster(cluster: EmbeddingCluster, prefix: String): List<FormattedTextNode> {
        val result = mutableListOf<FormattedTextNode>()
        if (cluster.baseChunk != null) {
            result.add(FormattedTextNode(prefix + "  - " + cluster.baseChunk.text))
        } else {
            result.add(FormattedTextNode(prefix + cluster.name))
            result.add(FormattedTextNode(prefix + "Theme: " + cluster.description.theme.toString()))
            result.add(FormattedTextNode(prefix + "Categories: " + cluster.description.categories.toString()))
            cluster.items.forEach {
                result.addAll(printCluster(it, "$prefix  "))
            }
        }
        return result
    }

    override fun loadTextLibrary(library: TextLibraryInfo) {
        model.loadTextLibrary(library, replace = false, selectAllDocs = true)
    }
}

