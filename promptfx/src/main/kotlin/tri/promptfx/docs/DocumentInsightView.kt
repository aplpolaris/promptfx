/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
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
package tri.promptfx.docs

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.layout.Priority
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.embedding.LocalFolderEmbeddingIndex
import tri.ai.pips.AiPlanner
import tri.ai.pips.AiTask
import tri.ai.pips.aggregate
import tri.ai.pips.tasks
import tri.ai.prompt.PromptTemplate
import tri.ai.prompt.trace.batch.AiPromptBatchCyclic
import tri.ai.text.chunks.BrowsableSource
import tri.ai.text.chunks.TextLibrary
import tri.promptfx.AiPlanTaskView
import tri.promptfx.PromptFxModels
import tri.promptfx.TextLibraryReceiver
import tri.promptfx.library.TextLibraryInfo
import tri.promptfx.ui.DocumentListView
import tri.promptfx.ui.EditablePromptUi
import tri.promptfx.ui.chunk.TextChunkListModel
import tri.promptfx.ui.chunk.TextChunkListView
import tri.promptfx.ui.chunk.asTextChunkViewModel
import tri.promptfx.ui.editablepromptui
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance
import tri.util.ui.slider
import tri.util.ui.sliderwitheditablelabel
import java.io.File

/** Plugin for the [DocumentQaView]. */
class DocumentInsightPlugin : NavigableWorkspaceViewImpl<DocumentInsightView>("Documents", "Document Insights", WorkspaceViewAffordance.COLLECTION_ONLY, DocumentInsightView::class)

/** A view that allows the user to run a template-based script against document(s). */
class DocumentInsightView: AiPlanTaskView(
    "Document Insights",
    "Use a template to extract information from a collection of documents",
), TextLibraryReceiver {

    private val viewScope = Scope(workspace)
    private lateinit var mapPromptUi: EditablePromptUi
    private lateinit var reducePromptUi: EditablePromptUi

    // selection of source documents
    private val documentLibrary = SimpleObjectProperty<TextLibrary>(null)
    private val documentFolder = SimpleObjectProperty(File(""))
    private val maxChunkSize = SimpleIntegerProperty(5000)
    private val embeddingIndex = Bindings.createObjectBinding({
        LocalFolderEmbeddingIndex(documentFolder.value, controller.embeddingStrategy.value).apply {
            maxChunkSize = this@DocumentInsightView.maxChunkSize.value
        }
    }, controller.embeddingStrategy, documentFolder, maxChunkSize)

    private val docs = observableListOf<BrowsableSource>()
    private val chunkListModel: TextChunkListModel by inject(viewScope)

    // for processing chunks to generate results
    private val docsToProcess = SimpleIntegerProperty(2)
    private val chunksToProcess = SimpleIntegerProperty(10)
    private val minSnippetCharsToProcess = SimpleIntegerProperty(50)

    // result of map processing step
    private val mapResult = SimpleStringProperty("")
    private val reduceResult = SimpleStringProperty("")

    init {
        preferences(PREF_APP) {
            documentFolder.value = File(get(PREF_DOCS_FOLDER, "docs/"))
        }
        documentFolder.onChange {
            preferences(PREF_APP) { put(PREF_DOCS_FOLDER, it!!.absolutePath) }
        }
    }

    init {
        input {
            mapPromptUi = editablepromptui(DOCUMENT_MAP_PREFIX, "Prompt for each snippet:")
            reducePromptUi = editablepromptui(DOCUMENT_REDUCE_PREFIX, "Prompt to summarize results:")
            add(DocumentListView(docs, hostServices))
            add(find<TextChunkListView>(viewScope).apply {
                label.set("Document Snippets")
            })
        }
    }

    init {
        documentsourceparameters(documentLibrary, documentFolder, maxChunkSize,
            reindexOp = { embeddingIndex.value!!.reindexAll() }
        )
        parameters("Document Snippet Aggregation") {
            field("Limit documents to") {
                tooltip("Max number of documents to process")
                slider(1..50, docsToProcess)
                label(docsToProcess)
            }
            field("Limit snippets per doc to") {
                tooltip("Max number of snippets per document to process")
                slider(1..50, chunksToProcess)
                label(chunksToProcess)
            }
            field("Minimum snippet size (chars)") {
                tooltip("Minimum size to process")
                sliderwitheditablelabel(1..5000, minSnippetCharsToProcess)
            }
        }
        addDefaultTextCompletionParameters(common)
    }

    init {
        outputPane.clear()
        output {
            textarea(mapResult) {
                promptText = "Results from processing document snippets will be shown here"
                isEditable = false
                isWrapText = true
                vgrow = Priority.ALWAYS
            }
            textarea(reduceResult) {
                promptText = "Combined result will be shown here"
                isEditable = false
                isWrapText = true
                vgrow = Priority.ALWAYS
            }
        }
        onCompleted {
            val pairResult = it.finalResult.firstValue as Pair<*, *>
            mapResult.value = pairResult.first.toString()
            reduceResult.value = pairResult.second.toString()
        }
    }

    override fun plan(): AiPlanner {
        mapResult.set("")
        reduceResult.set("")

        return promptBatch().aggregate()
            .aitask("results-summarize") { _ ->
                val concat = mapResult.value
                common.completionBuilder()
                    .text(reducePromptUi.fill(PromptTemplate.INPUT to concat))
                    .execute(completionEngine)
                    .mapOutput { concat to it }
            }.planner
    }

    private fun promptBatch(): List<AiTask<String>> {
        val snippets = updateDocs()
        val limitedSnippets = snippets.groupBy { it.first }
            .mapValues { it.value.take(chunksToProcess.value) }
            .values.flatten()

        return AiPromptBatchCyclic("processing-snippets").apply {
            var i = 1
            val names = limitedSnippets.map { "${it.second.browsable()!!.shortName} ${i++}" }
            val inputs = limitedSnippets.map { it.first.text(it.second.all) }
            model = completionEngine.modelId
            modelParams = common.toModelParams()
            prompt = mapPromptUi.templateText.value
            promptParams = mapOf(PromptTemplate.INPUT to inputs, "name" to names)
            runs = inputs.size
        }.tasks { id ->
            PromptFxModels.chatModels().find { it.modelId == id }!!
        }.map {
            // wrap each task to monitor output and update the UI with interim results
            it.monitor { res ->
                runLater { mapResult.value += "\n\n${res.first()}" }
            }
        }
    }

    override fun loadTextLibrary(library: TextLibraryInfo) {
        documentLibrary.set(library.library)
    }

    private fun updateDocs() = runBlocking {
        val sourceDocs = if (documentLibrary.value != null)
            documentLibrary.value!!.docs
        else
            embeddingIndex.value!!.calculateAndGetDocs()
        val docList = sourceDocs.take(docsToProcess.value)
        val chunkList = docList.flatMap { doc ->
            doc.chunks.take(chunksToProcess.value).map { it to doc }
        }
        runLater {
            val modelId = controller.embeddingStrategy.value.modelId
            docs.setAll(docList.map { it.browsable() })
            chunkListModel.chunkList.setAll(chunkList.map { it.asTextChunkViewModel(modelId) })
        }
        chunkList
    }

    companion object {
        private const val PREF_APP = "promptfx"
        private const val PREF_DOCS_FOLDER = "document-insights.folder"

        private const val DOCUMENT_MAP_PREFIX = "document-map"
        internal const val DOCUMENT_REDUCE_PREFIX = "document-reduce"
    }
}
