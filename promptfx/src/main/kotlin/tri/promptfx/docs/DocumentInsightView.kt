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

import javafx.application.Platform
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.pips.AiPipelineResult
import tri.ai.pips.AiPlanner
import tri.ai.pips.AiTask
import tri.ai.pips.aggregate
import tri.ai.pips.tasks
import tri.ai.prompt.PromptTemplate
import tri.ai.prompt.template
import tri.ai.prompt.trace.AiOutput
import tri.ai.prompt.trace.batch.AiPromptBatchCyclic
import tri.promptfx.AiPlanTaskView
import tri.promptfx.PromptFxConfig
import tri.promptfx.PromptFxGlobals.promptsWithPrefix
import tri.promptfx.PromptFxModels
import tri.promptfx.TextLibraryReceiver
import tri.promptfx.ui.PromptSelectionModel
import tri.promptfx.ui.chunk.TextChunkListView
import tri.promptfx.ui.chunk.TextChunkViewModel
import tri.promptfx.ui.docs.TextDocListUi
import tri.promptfx.ui.docs.TextLibraryToolbar
import tri.promptfx.ui.docs.TextLibraryViewModel
import tri.promptfx.ui.promptfield
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance
import tri.util.ui.slider
import tri.util.ui.sliderwitheditablelabel
import java.util.concurrent.FutureTask

/** Plugin for the [DocumentQaView]. */
class DocumentInsightPlugin : NavigableWorkspaceViewImpl<DocumentInsightView>("Documents", "Document Insights", WorkspaceViewAffordance.COLLECTION_ONLY, DocumentInsightView::class)

/** A view that allows the user to run a template-based script against document(s). */
class DocumentInsightView: AiPlanTaskView(
    "Document Insights",
    "Use a template to extract information from a collection of documents",
), TextLibraryReceiver {

    private val viewScope = Scope(workspace)

    // selection of source documents
    val model by inject<TextLibraryViewModel>(viewScope)

    // for processing chunks to generate results
    private val docProcessingLimit = SimpleIntegerProperty(2)
    private val chunkProcessingLimit = SimpleIntegerProperty(10)
    private val minProcessingSize = SimpleIntegerProperty(50)

    // processing operations and results
    private val mapPrompt = PromptSelectionModel("$DOCUMENT_MAP_PREFIX/custom")
    private val reducePrompt = PromptSelectionModel("$DOCUMENT_REDUCE_PREFIX/custom")
    private val mapResult = SimpleStringProperty("")
    private val reduceResult = SimpleStringProperty("")

    init {
        val prefLibraryFile = find<PromptFxConfig>().documentInsightFile()
        if (prefLibraryFile?.exists() == true)
            model.loadLibraryFrom(prefLibraryFile, replace = true, selectAllDocs = true)
    }

    init {
        input {
            add(find<TextDocListUi>(viewScope))
            add(find<TextChunkListView>(viewScope).apply {
                label.set("Document Snippets")
            })
        }
    }

    init {
        parameters("Document Library") {
            add(find<TextLibraryToolbar>(viewScope))
        }
        parameters("Prompt Processors") {
            promptfield("For Each Chunk", mapPrompt, promptsWithPrefix(DOCUMENT_MAP_PREFIX), workspace)
            promptfield("Combine", reducePrompt, promptsWithPrefix(DOCUMENT_REDUCE_PREFIX), workspace)
        }
        parameters("Processing Limits") {
            field("Limit documents to") {
                tooltip("Max number of documents to process")
                slider(1..50, docProcessingLimit)
                label(docProcessingLimit)
            }
            field("Limit snippets per doc to") {
                tooltip("Max number of snippets per document to process")
                slider(1..50, chunkProcessingLimit)
                label(chunkProcessingLimit)
            }
            field("Minimum snippet size (chars)") {
                tooltip("Minimum size to process")
                sliderwitheditablelabel(1..5000, minProcessingSize)
            }
        }
        addDefaultChatParameters(common)
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

    override suspend fun processUserInput(): AiPipelineResult {
        updateChunkSelection()
        return super.processUserInput()
    }

    override fun plan(): AiPlanner {
        mapResult.set("")
        reduceResult.set("")

        return promptBatch(model.chunkListModel.chunkSelection).aggregate()
            .aitask("results-summarize") { _ ->
                val concat = mapResult.value
                common.completionBuilder()
                    .prompt(reducePrompt.prompt.value)
                    .paramsInput(concat)
                    .execute(chatEngine)
                    .mapOutput { AiOutput(other = concat to it.content()) }
            }.planner
    }

    private fun promptBatch(chunks: List<TextChunkViewModel>): List<AiTask> {
        return AiPromptBatchCyclic("processing-snippets").apply {
            var i = 1
            val names = chunks.map { "${it.browsable!!.shortName} ${i++}" }
            val inputs = chunks.map { it.text }
            model = completionEngine.modelId
            modelParams = common.toModelParams()
            prompt = mapPrompt.prompt.value.template()
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
        model.loadTextLibrary(library)
    }

    private fun updateChunkSelection() {
        val task = FutureTask {
            model.docSelection.setAll(model.docSelection.take(docProcessingLimit.value))
            model.chunkListModel.setChunkList(
                model.docSelection.flatMap { doc -> doc.chunks.take(chunkProcessingLimit.value).map { it to doc } }
            )
            model.chunkListModel.chunkSelection.setAll(model.chunkListModel.chunkList)
        }
        Platform.runLater(task)
        task.get()
    }

    companion object {
        private const val DOCUMENT_MAP_PREFIX = "docs-map"
        internal const val DOCUMENT_REDUCE_PREFIX = "docs-reduce"
    }
}
