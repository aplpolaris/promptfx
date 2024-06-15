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
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.application.HostServices
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.input.DataFormat
import javafx.scene.layout.Priority
import javafx.scene.text.TextFlow
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.embedding.LocalFolderEmbeddingIndex
import tri.ai.openai.jsonMapper
import tri.ai.prompt.AiPromptLibrary
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.text.chunks.BrowsableSource
import tri.ai.text.chunks.TextLibrary
import tri.promptfx.AiPlanTaskView
import tri.promptfx.PromptFxConfig.Companion.DIR_KEY_TEXTLIB
import tri.promptfx.PromptFxConfig.Companion.FF_ALL
import tri.promptfx.PromptFxConfig.Companion.FF_JSON
import tri.promptfx.TextLibraryReceiver
import tri.promptfx.promptFxFileChooser
import tri.promptfx.promptTraceContextMenu
import tri.promptfx.tools.TextLibraryInfo
import tri.promptfx.ui.PromptSelectionModel
import tri.promptfx.ui.TextChunkListView
import tri.promptfx.ui.matchViewModel
import tri.promptfx.ui.promptfield
import tri.util.info
import tri.util.ui.*
import java.io.File

/** Plugin for the [DocumentQaView]. */
class DocumentQaPlugin : NavigableWorkspaceViewImpl<DocumentQaView>("Documents", "Document Q&A", WorkspaceViewAffordance.INPUT_AND_COLLECTION, DocumentQaView::class)

/** A view that allows the user to ask a question about a document, and the system will find the most relevant section. */
class DocumentQaView: AiPlanTaskView(
    "Document Q&A",
    "Enter question below to respond based on content of documents in a specified folder.",
), TextLibraryReceiver {

    private val prompt = PromptSelectionModel("$PROMPT_PREFIX-docs")
    private val joinerPrompt = PromptSelectionModel("$JOINER_PREFIX-citations")

    val question = SimpleStringProperty("")

    private val documentLibrary = SimpleObjectProperty<TextLibrary>(null)
    val documentFolder = SimpleObjectProperty(File(""))
    private val maxChunkSize = SimpleIntegerProperty(1000)
    private val chunksToRetrieve = SimpleIntegerProperty(10)
    private val minChunkSizeForRelevancy = SimpleIntegerProperty(50)
    private val chunksToSendWithQuery = SimpleIntegerProperty(5)

    val planner = DocumentQaPlanner().apply {
        documentLibrary = this@DocumentQaView.documentLibrary
        embeddingIndex = controller.embeddingService.objectBinding(documentFolder, maxChunkSize) {
            LocalFolderEmbeddingIndex(documentFolder.value, it!!).apply {
                maxChunkSize = this@DocumentQaView.maxChunkSize.value
            }
        }
    }
    val snippets
        get() = planner.snippets

    private val htmlResult = SimpleStringProperty("")
    private val resultTrace = SimpleObjectProperty<AiPromptTrace>()

    private lateinit var resultBox: TextFlow

    init {
        preferences(PREF_APP) {
            documentFolder.value = File(get(PREF_DOCS_FOLDER, "docs/"))
        }
        documentFolder.onChange {
            preferences(PREF_APP) { put(PREF_DOCS_FOLDER, it!!.absolutePath) }
        }
    }

    init {
        addInputTextArea(question) {
            style = "-fx-font-size: 18px;"
        }
        input {
            hbox {
                alignment = Pos.CENTER_LEFT
                spacing = 5.0
                paddingAll = 5.0
                text("Document Snippets:")
                spacer()
                // export JSON with matches
                button("", FontAwesomeIconView(FontAwesomeIcon.DOWNLOAD)) {
                    disableWhen(planner.snippets.sizeProperty.isEqualTo(0))
                    action { exportDocumentSnippets() }
                }
            }
            add(TextChunkListView(planner.snippets.matchViewModel(), hostServices))
        }
        documentsourceparameters(documentLibrary, documentFolder, maxChunkSize,
            reindexOp = { planner.reindexAllDocuments() }
        )
        parameters("Document Snippet Matching and Query") {
            field("# Matches") {
                tooltip("Number of matching snippets to retrieve from the document database")
                slider(1..100, chunksToRetrieve)
                label(chunksToRetrieve)
            }
            field("Minimum chars") {
                tooltip("Snippets with a character count below this limit will be ignored")
                sliderwitheditablelabel(1..1000, minChunkSizeForRelevancy)
                label(minChunkSizeForRelevancy)
            }
            field("Query snippets") {
                tooltip("Number of matching snippets to send to the question answering engine")
                slider(1..50, chunksToSendWithQuery)
                label(chunksToSendWithQuery)
            }
        }
        addDefaultTextCompletionParameters(common)
        parameters("Prompt Template") {
            tooltip("Loads from prompts.yaml with prefix $PROMPT_PREFIX and $JOINER_PREFIX")
            promptfield("Template", prompt, AiPromptLibrary.withPrefix(PROMPT_PREFIX), workspace)
            promptfield("Snippet Joiner", joinerPrompt, AiPromptLibrary.withPrefix(JOINER_PREFIX), workspace)
        }

        outputPane.clear()
        output {
            scrollpane {
                vgrow = Priority.ALWAYS
                isFitToWidth = true
                resultBox = textflow {
                    padding = insets(5.0)
                    vgrow = Priority.ALWAYS
                    style = "-fx-font-size: 16px;"

                    promptTraceContextMenu(this@DocumentQaView, resultTrace) {
                        item("Copy output to clipboard") {
                            action {
                                clipboard.setContent(mapOf(
                                    DataFormat.HTML to htmlResult.value,
                                    DataFormat.PLAIN_TEXT to plainText()
                                ))
                            }
                        }
                    }
                }
            }
        }
        onCompleted {
            val fr = it.finalResult as FormattedPromptTraceResult
            htmlResult.set(fr.text.toHtml())
            resultTrace.set(fr.trace)
            resultBox.children.clear()
            resultBox.children.addAll(fr.text.toFxNodes())
        }
    }

    override fun plan() = planner.plan(
        question = question.value,
        promptId = prompt.id.value,
        embeddingService = controller.embeddingService.value,
        chunksToRetrieve = chunksToRetrieve.value,
        minChunkSize = minChunkSizeForRelevancy.value,
        contextStrategy = GroupingTemplateJoiner(joinerPrompt.id.value),
        contextChunks = chunksToSendWithQuery.value,
        completionEngine = controller.completionEngine.value,
        maxTokens = common.maxTokens.value,
        tempParameters = common
    )

    //region ACTIONS

    override fun loadTextLibrary(library: TextLibraryInfo) {
        documentLibrary.set(library.library)
    }

    private fun exportDocumentSnippets() {
        promptFxFileChooser(
            dirKey = DIR_KEY_TEXTLIB,
            title = "Export Document Snippets as JSON",
            filters = arrayOf(FF_JSON, FF_ALL),
            mode = FileChooserMode.Save
        ) {
            if (it.isNotEmpty()) {
                runAsync {
                    runBlocking {
                        jsonMapper
                            .writerWithDefaultPrettyPrinter()
                            .writeValue(it.first(), planner.lastResult)
                    }
                }
            }
        }
    }

    //endregion

    // override the user input with post-processing for hyperlinks
    override suspend fun processUserInput() =
        super.processUserInput().also {
            val ft = (it.finalResult as FormattedPromptTraceResult).text
            ft.hyperlinkOp = { docName ->
                val doc = snippets.firstOrNull { it.shortDocName == docName }?.document?.browsable()
                if (doc == null) {
                    tri.util.warning<DocumentQaView>("Unable to find document $docName in snippets.")
                } else {
                    browseToBestSnippet(doc, planner.lastResult, hostServices)
                }
            }
        }

    companion object {
        private const val PREF_APP = "promptfx"
        private const val PREF_DOCS_FOLDER = "document-qa.folder"

        private const val PROMPT_PREFIX = "question-answer"
        private const val JOINER_PREFIX = "snippet-joiner"

        internal fun browseToBestSnippet(doc: BrowsableSource, result: QuestionAnswerResult?, hostServices: HostServices) {
            if (doc.uri.scheme.startsWith("http")) {
                info<DocumentQaView>("Browsing to website: ${doc.uri}")
                hostServices.showDocument(doc.uri.toString())
            } else if (result == null) {
                info<DocumentQaView>("Browsing to first page: ${doc.shortNameWithoutExtension}")
                DocumentOpenInViewer(doc, hostServices).open()
            } else {
                info<DocumentQaView>("Browsing to best snippet: ${doc.shortNameWithoutExtension}")
                val matches = result.matches.filter { it.shortDocName == doc.shortNameWithoutExtension }
                if (matches.size == 1) {
                    info<DocumentQaView>("Browsing to only match")
                    val match = matches.first()
                    DocumentBrowseToPage(match.document.browsable()!!, match.chunkText, hostServices).open()
                } else {
                    info<DocumentQaView>("Browsing to closest match")
                    DocumentBrowseToClosestMatch(matches, result.responseEmbedding, hostServices).open()
                }
            }
        }
    }
}