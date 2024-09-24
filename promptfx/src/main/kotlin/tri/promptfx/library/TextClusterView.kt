package tri.promptfx.library

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Orientation
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.pips.*
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiPromptTraceSupport
import tri.promptfx.AiPlanTaskView
import tri.promptfx.AiTaskView
import tri.promptfx.TextLibraryReceiver
import tri.promptfx.library.TextClustering.generateClusterHierarchy
import tri.promptfx.ui.FormattedText
import tri.promptfx.ui.FormattedTextNode
import tri.promptfx.ui.chunk.TextChunkListView
import tri.promptfx.ui.docs.TextDocListUi
import tri.promptfx.ui.docs.TextLibraryListUi
import tri.promptfx.ui.docs.TextLibraryViewModel
import tri.util.info
import tri.util.ml.AffinityClusterService
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance
import tri.util.ui.sliderwitheditablelabel

/** Plugin for the [TextClusterView]. */
class TextClusterPlugin : NavigableWorkspaceViewImpl<TextClusterView>("Documents", "Text Clustering", WorkspaceViewAffordance.COLLECTION_ONLY, TextClusterView::class)

/** View designed to create clusters of selected text chunks. */
class TextClusterView : AiPlanTaskView("Text Clustering", "Cluster documents and text."), TextLibraryReceiver {

    val viewScope = Scope(workspace)
    val model by inject<TextLibraryViewModel>(viewScope)

    private val maxChunksToCluster = SimpleIntegerProperty(100)
    private val generateSummary = SimpleBooleanProperty(false)
    private val summarizeMaxChars = SimpleIntegerProperty(1000)
    private val summarizeSample = SimpleStringProperty("This content all appears to discuss animals or pets.")
    private val generateCategories = SimpleBooleanProperty(false)
    private val categoryList = SimpleStringProperty("")
    private val inputType = SimpleStringProperty("text snippets")
    private val minForRegroup = SimpleIntegerProperty(20)
    private val attempts = SimpleIntegerProperty(3)

    init {
        input {
            splitpane(Orientation.VERTICAL) {
                vgrow = Priority.ALWAYS
                add(find<TextLibraryListUi>(viewScope))
                add(find<TextDocListUi>(viewScope))
                add(find<TextChunkListView>(viewScope))
            }
        }

        parameters("Clustering Parameters") {
            field("Max # Chunks to Cluster") {
                tooltip("Number of text chunks to use for clustering")
                sliderwitheditablelabel(1..1000, maxChunksToCluster)
            }
            field("Min # Chunks for Regroup") {
                tooltip("Minimum number of chunks to regroup")
                sliderwitheditablelabel(2..100, minForRegroup)
            }
        }

        parameters("Automated Summarization of Clusters") {
            field("Generate Summaries") {
                tooltip("Select to enable automated cluster summarization")
                checkbox("", generateSummary)
            }
            field("Input Type") {
                tooltip("Provide a more specific description of the input (which can lead to better results)")
                textfield(inputType)
                enableWhen(generateSummary.or(generateCategories))
            }
            field("# Attempts") {
                tooltip("Number of attempts to generate cluster summaries")
                sliderwitheditablelabel(1..10, attempts)
                enableWhen(generateSummary.or(generateCategories))
            }
            field("Max Input Chars") {
                tooltip("Maximum number of characters in a cluster summary")
                sliderwitheditablelabel(1..10000, summarizeMaxChars)
                enableWhen(generateSummary)
            }
            field("Sample Summary") {
                tooltip("Provide an example of the kind of summary ")
                textarea(summarizeSample) {
                    prefColumnCount = 16
                    prefRowCount = 3
                    isWrapText = true
                }
                enableWhen(generateSummary)
            }
            field("Generate Categories") {
                tooltip("Select to enable automated cluster categorization")
                checkbox("", generateCategories)
            }
            field("List of Categories") {
                tooltip("Comma-separated list of categories")
                textfield(categoryList)
                enableWhen(generateCategories)
            }
        }
    }

    override fun plan() =
        aitasklist(model.calculateEmbeddingsPlan().plan() as List<AiTask<String>>)
        .aitask("clustering") {
            val t0 = System.currentTimeMillis()
            val chunks = model.chunkListModel.filteredChunkList.toList()
            val summaryType = when {
                generateSummary.value && generateCategories.value -> ClusterSummaryType.CATEGORIES_AND_THEME
                generateSummary.value -> ClusterSummaryType.THEME_ONLY
                generateCategories.value -> ClusterSummaryType.CATEGORIES_ONLY
                else -> ClusterSummaryType.NONE
            }
            val hierarchy = AffinityClusterService().generateClusterHierarchy(
                chunks,
                summaryType,
                itemType = inputType.value,
                categories = categoryList.value.split(",").map { it.trim() },
                sampleTheme = summarizeSample.value.ifBlank { "This content all appears to discuss animals or pets." },
                completionEngine = controller.completionEngine.value,
                embeddingService = controller.embeddingService.value,
                minForRegroup = minForRegroup.value,
                attempts = attempts.value,
                progress = { msg, pct ->
                    progress.progressUpdate(msg, pct)
                    info<TextClusterView>("  $msg: %.2f%%".format(pct * 100))
                }
            )
            AiPromptTrace(execInfo = AiExecInfo.durationSince(t0), outputInfo = AiOutputInfo.output(hierarchy))
        }
        .task("formatting-results") {
            FormattedText(it.map { printCluster(it, "\n") }.flatten())
        }.planner

    //region PRETTY PRINT

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

    //endregion

    override fun loadTextLibrary(library: TextLibraryInfo) {
        model.loadTextLibrary(library, replace = false, selectAllDocs = true)
    }
}

