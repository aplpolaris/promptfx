/*-
 * #%L
 * promptfx-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.TextArea
import javafx.scene.layout.Priority
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.core.TextPlugin
import tri.ai.embedding.EmbeddingDocument
import tri.ai.embedding.EmbeddingSectionInDocument
import tri.ai.embedding.LocalEmbeddingIndex
import tri.ai.pips.AiPlanner
import tri.ai.pips.AiTask
import tri.ai.pips.AiTask.Companion.aitask
import tri.ai.pips.AiTaskList
import tri.ai.prompt.AiPrompt.Companion.fill
import tri.ai.prompt.AiPromptLibrary
import tri.ai.prompt.AiPromptLibrary.Companion.lookupPrompt
import tri.promptfx.AiPlanTaskView
import tri.promptfx.ui.EditablePromptUi
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
    private val embeddingIndex = controller.embeddingService.objectBinding(documentFolder, maxChunkSize) {
        LocalEmbeddingIndex(documentFolder.value, it!!).apply {
            maxChunkSize = this@DocumentInsightView.maxChunkSize.value
        }
    }
    private val docs = observableListOf<EmbeddingDocument>()
    private val snippets = observableListOf<EmbeddingSectionInDocument>()

    // for processing chunks to generate results
    private val docsToProcess = SimpleIntegerProperty(2)
    private val snippetsToProcess = SimpleIntegerProperty(10)
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
                        mapPromptUi = EditablePromptUi("document-map", "Prompt for each snippet:")
                        reducePromptUi = EditablePromptUi("document-reduce", "Prompt to summarize results:")
                        add(mapPromptUi)
                        add(reducePromptUi)
                    }
                }
                fold("Documents", expanded = true) {
                    docslist(docs, hostServices)
                }
                fold("Snippets", expanded = true) {
                    snippetlist(snippets, hostServices)
                }
            }
        }
        parameters("Document Source and Sectioning") {
            field("Folder") {
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
                    action { documentFolder.chooseFolder(currentStage) }
                }
                button("", FontAwesomeIcon.GLOBE.graphic) {
                    tooltip("Enter a website to scrape")
                    action { find<TextCrawlDialog>(params = mapOf("folder" to documentFolder)).openModal() }
                }
                button("", FontAwesomeIcon.REFRESH.graphic) {
                    tooltip("Rebuild embedding index for this folder")
                    action {
                        // confirm with user then refresh
                        confirm("Rebuild Embedding Index",
                            "Are you sure you want to rebuild the entire embedding index?\n" +
                                    "This may require significant API usage and cost.") {
                            runAsync {
                                runBlocking { embeddingIndex.value!!.reindexAll() }
                            }
                        }
                    }
                }
            }
            field("Max snippet size") {
                tooltip("Maximum number of characters to include in a chunked section of the document for the embedding index.\n" +
                        "This will only apply to newly chunked documents.")
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
                slider(1..50, snippetsToProcess)
                label(snippetsToProcess)
            }
            field("Minimum snippet size (chars)") {
                tooltip("Minimum size to process")
                slider(1..5000, minSnippetCharsToProcess)
                label(minSnippetCharsToProcess)
            }
        }
        parameters("Model") {
            field("Model") {
                combobox(controller.completionEngine, TextPlugin.textCompletionModels())
            }
            with (common) {
                temperature()
                maxTokens()
            }
        }

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
        val snippets = updateDocs()
        val limitedSnippets = snippets.groupBy { it.doc }
            .mapValues { it.value.take(snippetsToProcess.value) }
            .values.flatten()
        val plans = limitedSnippets.map {
            aitask("${it.doc.shortName} ${it.section.start} ${it.section.end}") {
                val res = completionEngine.complete(
                    mapPromptUi.templateText.value.fill("input" to it.readText()),
                    common.maxTokens.value,
                    common.temp.value
                )
                runLater { mapResult.value += "\n\n" + res.value }
                res
            }
        }
        val finalTask = plans.aitask("results-summarize") {
            val concat = it.values.joinToString("\n\n") { it.value as String }
            runLater { mapResult.value = concat }
            completionEngine.complete(
                reducePromptUi.templateText.value.fill("input" to concat),
                common.maxTokens.value,
                common.temp.value
            ).map { concat to it }
        }
        return AiTaskList(plans, finalTask).planner
    }

    private fun updateDocs() = runBlocking {
        val docList = embeddingIndex.value!!.getEmbeddingIndex().values.take(docsToProcess.value)
        val embeddingList = docList.flatMap { doc ->
            doc.sections.take(snippetsToProcess.value)
                .map { EmbeddingSectionInDocument(doc, it) }
        }
        runLater {
            docs.setAll(docList)
            snippets.setAll(embeddingList)
        }
        embeddingList
    }

    companion object {
        private const val PREF_APP = "promptfx"
        private const val PREF_DOCS_FOLDER = "document-insights.folder"
    }
}