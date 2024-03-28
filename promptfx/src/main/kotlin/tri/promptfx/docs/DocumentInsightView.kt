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
package tri.promptfx.docs

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.embedding.LocalFolderEmbeddingIndex
import tri.ai.pips.*
import tri.ai.prompt.trace.batch.AiPromptBatchCyclic
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.text.chunks.BrowsableSource
import tri.ai.text.chunks.TextChunk
import tri.ai.text.chunks.TextDoc
import tri.promptfx.AiPlanTaskView
import tri.promptfx.promptFxDirectoryChooser
import tri.promptfx.ui.DocumentListView
import tri.promptfx.ui.EditablePromptUi
import tri.promptfx.ui.TextChunkListView
import tri.promptfx.ui.sectionViewModel
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.graphic
import tri.util.ui.slider
import java.awt.Desktop
import java.io.File
import java.nio.file.Files

/** Plugin for the [DocumentQaView]. */
class DocumentInsightPlugin : NavigableWorkspaceViewImpl<DocumentInsightView>("Documents", "Document Insights", DocumentInsightView::class)

/** A view that allows the user to run a template-based script against document(s). */
class DocumentInsightView: AiPlanTaskView(
    "Document Insights",
    "Use a template to extract information from a collection of documents",
) {

    private lateinit var mapPromptUi: EditablePromptUi
    private lateinit var reducePromptUi: EditablePromptUi

    // selection of source documents
    private val documentFolder = SimpleObjectProperty(File(""))
    private val maxChunkSize = SimpleIntegerProperty(5000)
    private val embeddingIndex = Bindings.createObjectBinding({
        LocalFolderEmbeddingIndex(documentFolder.value, controller.embeddingService.value).apply {
            maxChunkSize = this@DocumentInsightView.maxChunkSize.value
        }
    }, controller.embeddingService, documentFolder, maxChunkSize)
    private val docs = observableListOf<BrowsableSource>()
    private val snippets = observableListOf<Pair<TextDoc, TextChunk>>()

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
            squeezebox {
                fold("Prompts", expanded = true) {
                    vbox {
                        mapPromptUi = EditablePromptUi(DOCUMENT_MAP_PREFIX, "Prompt for each snippet:")
                        reducePromptUi = EditablePromptUi(DOCUMENT_REDUCE_PREFIX, "Prompt to summarize results:")
                        add(mapPromptUi)
                        add(reducePromptUi)
                    }
                }
                fold("Documents", expanded = true) {
                    add(DocumentListView(docs, hostServices))
                }
                fold("Snippets", expanded = true) {
                    add(TextChunkListView(snippets.sectionViewModel(embeddingService.modelId), hostServices))
                }
            }
        }
    }

    init {
        parameters("Document Source and Sectioning") {
            field("Folder") {
                (inputContainer as? HBox)?.spacing = 5.0
                hyperlink(documentFolder.stringBinding {
                    val path = it!!.absolutePath
                    if (path.length > 25) {
                        "..." + path.substring(path.length - 24)
                    } else {
                        path
                    }
                }) {
                    action {
                        Files.createDirectories(documentFolder.get().toPath())
                        Desktop.getDesktop().open(documentFolder.get())
                    }
                }
                button("", FontAwesomeIcon.FOLDER_OPEN.graphic) {
                    tooltip("Select folder with documents for Q&A")
                    action { promptFxDirectoryChooser { documentFolder.set(it) } }
                }
                button("", FontAwesomeIcon.GLOBE.graphic) {
                    tooltip("Enter a website to scrape")
                    action { find<TextCrawlDialog>(params = mapOf("folder" to documentFolder)).openModal() }
                }
                button("", FontAwesomeIcon.REFRESH.graphic) {
                    tooltip("Rebuild embedding index for this folder")
                    action {
                        // confirm with user then refresh
                        confirm(
                            "Rebuild Embedding Index",
                            "Are you sure you want to rebuild the entire embedding index?\n" +
                                    "This may require significant API usage and cost."
                        ) {
                            runAsync {
                                runBlocking { embeddingIndex.value!!.reindexAll() }
                            }
                        }
                    }
                }
            }
            field("Max snippet size") {
                tooltip(
                    "Maximum number of characters to include in a chunked section of the document for the embedding index.\n" +
                            "This will only apply to newly chunked documents."
                )
                slider(500..5000, maxChunkSize)
                label(maxChunkSize)
            }
        }
        parameters("Document Snippet Processing") {
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
                slider(1..5000, minSnippetCharsToProcess)
                label(minSnippetCharsToProcess)
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
            val pairResult = it.finalResult as Pair<*, *>
            mapResult.value = pairResult.first.toString()
            reduceResult.value = pairResult.second.toString()
        }
    }

    override fun plan(): AiPlanner {
        mapResult.set("")
        reduceResult.set("")

        return promptBatch().aggregate()
            .aitask("results-summarize") { list: List<AiPromptTrace> ->
                val concat = list.mapNotNull { it.outputInfo.output }
                    .joinToString("\n\n")
                runLater { mapResult.value = concat }
                completionEngine.complete(
                    reducePromptUi.fill("input" to concat),
                    common.maxTokens.value,
                    common.temp.value
                ).map { concat to it }
            }.planner
    }

    private fun promptBatch(): List<AiTask<AiPromptTrace>> {
        val snippets = updateDocs()
        val limitedSnippets = snippets.groupBy { it.first }
            .mapValues { it.value.take(chunksToProcess.value) }
            .values.flatten()

        return AiPromptBatchCyclic("processing-snippets").apply {
            var i = 1
            val names = limitedSnippets.map { "${it.first.browsable()!!.shortName} ${i++}" }
            val inputs = limitedSnippets.map { it.second.text(it.first.all) }
            model = completionEngine.modelId
            modelParams = common.toModelParams()
            prompt = mapPromptUi.templateText.value
            promptParams = mapOf("input" to inputs, "name" to names)
            runs = inputs.size
        }.tasks().map {
            // wrap each task to monitor output and update the UI with interim results
            it.monitor { res ->
                res.outputInfo.output?.let {
                    runLater { mapResult.value += "\n\n$it" }
                }
            }
        }
    }

    private fun updateDocs() = runBlocking {
        val docList = embeddingIndex.value!!.calculateAndGetDocs().take(docsToProcess.value)
        val chunkList = docList.flatMap { doc ->
            doc.chunks.take(chunksToProcess.value).map { doc to it }
        }
        runLater {
            docs.setAll(docList.map { it.browsable() })
            snippets.setAll(chunkList)
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
