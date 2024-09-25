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
package tri.promptfx.library

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Orientation
import javafx.scene.layout.Priority
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.openai.jsonMapper
import tri.ai.pips.AiTask
import tri.ai.pips.aitasklist
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.promptfx.AiPlanTaskView
import tri.promptfx.PromptFxConfig.Companion.FF_ALL
import tri.promptfx.PromptFxConfig.Companion.FF_JSON
import tri.promptfx.TextLibraryReceiver
import tri.promptfx.library.TextClustering.generateClusterHierarchy
import tri.promptfx.promptFxFileChooser
import tri.promptfx.ui.FormattedPromptResultArea
import tri.promptfx.ui.FormattedPromptTraceResult
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
class TextClusterView : AiPlanTaskView("Text Clustering", "Cluster documents and text, optionally adding descriptions and categories to clusters."), TextLibraryReceiver {

    val viewScope = Scope(workspace)
    val model by inject<TextLibraryViewModel>(viewScope)

    private val maxChunksToCluster = SimpleIntegerProperty(100)
    private val generateSummary = SimpleBooleanProperty(false)
    private val summarizeSample = SimpleStringProperty("This content all appears to discuss animals or pets.")
    private val generateCategories = SimpleBooleanProperty(false)
    private val categoryList = SimpleStringProperty("")
    private val inputType = SimpleStringProperty("text snippets")
    private val minForRegroup = SimpleIntegerProperty(20)
    private val attempts = SimpleIntegerProperty(1)

    private val resultClusters = observableListOf<EmbeddingCluster>()
    private val hasResultClusters = resultClusters.sizeProperty.greaterThan(0)

    init {
        input {
            splitpane(Orientation.VERTICAL) {
                vgrow = Priority.ALWAYS
                add(find<TextLibraryListUi>(viewScope))
                add(find<TextDocListUi>(viewScope))
                add(find<TextChunkListView>(viewScope))
            }
        }

        val resultBox = FormattedPromptResultArea()
        outputPane.clear()
        output {
            vbox {
                toolbar {
                    button("Export...") {
                        enableWhen(hasResultClusters)
                        action { exportClusters() }
                    }
                }
                add(resultBox)
            }
        }
        onCompleted {
            resultBox.setFinalResult(it.finalResult as FormattedPromptTraceResult)
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
                tooltip("Number of attempts to generate cluster summaries (higher values may improve quality, especially for smaller models)")
                sliderwitheditablelabel(1..10, attempts)
                enableWhen(generateSummary.or(generateCategories))
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
        aitasklist(model.calculateEmbeddings().plan as List<AiTask<String>>)
        .aitask("clustering") {
            val t0 = System.currentTimeMillis()
            val chunks = model.chunkListModel.filteredChunkList.toList().take(maxChunksToCluster.value)
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
        .aitask("formatting-results") {
            runLater { resultClusters.setAll(it) }
            val ft = FormattedText(it.map { printCluster(it, "\n") }.flatten())
            FormattedPromptTraceResult(AiPromptTrace(outputInfo = AiOutputInfo.output("")), listOf(ft))
        }.planner

    //region PRETTY PRINT

    private fun printCluster(cluster: EmbeddingCluster, prefix: String): List<FormattedTextNode> {
        val result = mutableListOf<FormattedTextNode>()
        result.add(FormattedTextNode(prefix + cluster.name, style = CLUSTER_NAME_STYLE))
        result.add(FormattedTextNode(prefix + "Theme: " + cluster.description.theme.toString(), style = CLUSTER_THEME_STYLE))
        result.add(FormattedTextNode(prefix + "Categories: " + cluster.description.categories.toString(), style = CLUSTER_CATEGORIES_STYLE))
        if (cluster.items.all { it.baseChunk != null }) {
            cluster.items.forEach {
                result.add(FormattedTextNode(prefix + "  - " + it.baseChunk!!.text.trim(), style = CLUSTER_CHUNK_STYLE))
            }
            result.add(FormattedTextNode("\n", style = CLUSTER_CHUNK_STYLE))
        } else if (cluster.items.any { it.baseChunk != null }) {
            throw IllegalStateException("Cluster contains both base chunks and sub-clusters")
        } else {
            cluster.items.forEach {
                result.addAll(printCluster(it, "$prefix  "))
            }
        }
        return result
    }

    //endregion

    private fun exportClusters() {
        promptFxFileChooser(
            dirKey = "export-clusters",
            title = "Export Clusters as JSON",
            filters = arrayOf(FF_JSON, FF_ALL),
            mode = FileChooserMode.Save
        ) {
            if (it.isNotEmpty()) {
                runAsync {
                    runBlocking {
                        jsonMapper.writerWithDefaultPrettyPrinter()
                            .writeValue(it.first(), resultClusters)
                    }
                }
            }
        }
    }

    override fun loadTextLibrary(library: TextLibraryInfo) {
        model.loadTextLibrary(library, replace = false, selectAllDocs = true)
    }

    companion object {
        private const val CLUSTER_NAME_STYLE = "-fx-font-weight: bold; -fx-fill: blue; -fx-font-size: 1.2em;"
        private const val CLUSTER_THEME_STYLE = "-fx-font-style: italic; -fx-fill: darkgreen;"
        private const val CLUSTER_CATEGORIES_STYLE = "-fx-fill: gray; -fx-font-variant: small-caps;"
        private const val CLUSTER_CHUNK_STYLE = "-fx-fill: black; -fx-font-family: monospace; -fx-font-size: 0.8em;"
    }
}

