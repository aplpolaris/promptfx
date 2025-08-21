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

import javafx.application.HostServices
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.embedding.LocalFolderEmbeddingIndex
import tri.ai.pips.AiPipelineExecutor
import tri.ai.pips.AiPipelineResult
import tri.ai.prompt.trace.AiPromptTraceSupport
import tri.ai.text.chunks.BrowsableSource
import tri.ai.text.chunks.TextLibrary
import tri.ai.text.docs.FormattedPromptTraceResult
import tri.ai.text.docs.FormattedText
import tri.ai.text.docs.GroupingTemplateJoiner
import tri.ai.text.docs.QuestionAnswerResult
import tri.promptfx.AiPlanTaskView
import tri.promptfx.PromptFxGlobals.promptsWithPrefix
import tri.promptfx.TextLibraryReceiver
import tri.promptfx.ui.PromptSelectionModel
import tri.promptfx.ui.chunk.TextChunkListView
import tri.promptfx.ui.chunk.matchViewModel
import tri.promptfx.ui.promptfield
import tri.util.info
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance
import tri.util.ui.slider
import tri.util.ui.sliderwitheditablelabel
import java.io.File

/** Plugin for the [DocumentQaView]. */
class DocumentQaPlugin : NavigableWorkspaceViewImpl<DocumentQaView>("Documents", "Document Q&A", WorkspaceViewAffordance.INPUT_AND_COLLECTION, DocumentQaView::class)

/** A view that allows the user to ask a question about a document, and the system will find the most relevant section. */
class DocumentQaView: AiPlanTaskView(
    "Document Q&A",
    "Enter question below to respond based on content of documents in a specified folder.",
), TextLibraryReceiver {

    private val viewScope = Scope(workspace)
    private val prompt = PromptSelectionModel("$PROMPT_PREFIX/answer")
    private val joinerPrompt = PromptSelectionModel("$JOINER_PREFIX/citations")

    private val inputToggleGroup = ToggleGroup()
    private val singleInput = SimpleBooleanProperty(true)
    private val multiInput = SimpleBooleanProperty(false)
    val question = SimpleStringProperty("")

    private val documentLibrary = SimpleObjectProperty<TextLibrary>(null)
    val documentFolder = SimpleObjectProperty(File(""))
    private val maxChunkSize = SimpleIntegerProperty(1000)
    private val chunksToRetrieve = SimpleIntegerProperty(50)
    private val minChunkSizeForRelevancy = SimpleIntegerProperty(50)
    private val chunksToSendWithQuery = SimpleIntegerProperty(5)
    private val historySize = SimpleIntegerProperty(0)

    val planner = DocumentQaPlannerFx().apply {
        documentLibrary = this@DocumentQaView.documentLibrary
        embeddingIndex = controller.embeddingStrategy.objectBinding(documentFolder, maxChunkSize) {
            LocalFolderEmbeddingIndex(documentFolder.value, it!!).apply {
                maxChunkSize = this@DocumentQaView.maxChunkSize.value
            }
        }
        historySize = this@DocumentQaView.historySize
    }
    val snippets
        get() = planner.snippets

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
            vbox {
                toolbar {
                    radiobutton("Single input", inputToggleGroup) {
                        selectedProperty().bindBidirectional(singleInput)
                    }
                    radiobutton("Batch input", inputToggleGroup) {
                        selectedProperty().bindBidirectional(multiInput)
                    }
                }
                textarea(question) {
                    vgrow = Priority.ALWAYS
                    isWrapText = true
                    style = "-fx-font-size: 18px;"
                }
            }
        }
        input {
            add(find<TextChunkListView>(viewScope).apply {
                label.set("Document Snippets")
                planner.snippets.matchViewModel().onChange {
                    model.chunkList.setAll(it.list)
                }
            })
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
            }
            field("Query snippets") {
                tooltip("Number of matching snippets to send to the question answering engine")
                slider(1..50, chunksToSendWithQuery)
                label(chunksToSendWithQuery)
            }
            field("History size") {
                tooltip("Number of previous chat messages to send to the question answering engine (including both questions and responses)")
                slider(0..10, historySize)
                label(historySize)
            }
        }
        addDefaultChatParameters(common)
        parameters("Prompt Template") {
            tooltip("Loads prompts with prefix $PROMPT_PREFIX and $JOINER_PREFIX")
            promptfield("Template", prompt, promptsWithPrefix(PROMPT_PREFIX), workspace)
            promptfield("Snippet Joiner", joinerPrompt, promptsWithPrefix(JOINER_PREFIX), workspace)
        }

        outputPane.clear()
        outputPane.add(formattedResultArea)
        onCompleted.clear()
    }

    /**
     * Executes task on a background thread and updates progress info.
     * Overwrites parent method to allow for batch execution.
     */
    override fun runTask(op: suspend () -> AiPipelineResult<*>) {
        formattedResultArea.model.clearTraces()
        val questionInput = question.value
        val questions = if (singleInput.value) listOf(questionInput) else questionInput.split("\n").map { it.trim() }
            .filter { it.isNotBlank() }
        if (questions.isEmpty()) {
            error("No questions found.")
            return
        }
        questions.forEach {
            question.set(it)
            super.runTask {
                AiPipelineExecutor.execute(questionTaskList(it).planner.plan(), progress).also {
                    runLater {
                        addTrace(it.finalResult)
                    }
                }
            }
        }
        question.set(questionInput)
    }

    override fun plan() = questionTaskList(question.value).planner

    private fun questionTaskList(question: String) =
        planner.taskList(
            question = question,
            prompt = prompt.prompt.value,
            chunksToRetrieve = chunksToRetrieve.value,
            minChunkSize = minChunkSizeForRelevancy.value,
            contextStrategy = GroupingTemplateJoiner(joinerPrompt.id.value),
            contextChunks = chunksToSendWithQuery.value,
            chatEngine = controller.chatService.value,
            maxTokens = common.maxTokens.value,
            temp = common.temp.value,
            numResponses = common.numResponses.value
        )

    //region ACTIONS

    override fun loadTextLibrary(library: TextLibraryInfo) {
        documentLibrary.set(library.library)
    }

    //endregion

    override fun addTrace(trace: AiPromptTraceSupport<*>) {
        if (trace is FormattedPromptTraceResult) {
            enableHyperlinkActions(trace.formattedOutputs)
        }
        super.addTrace(trace)
    }

    /** Enable hyperlink actions for the trace. */
    internal fun enableHyperlinkActions(text: List<FormattedText>) {
        text.forEach {
            it.hyperlinkOp = { docName ->
                val doc = snippets.firstOrNull { it.shortDocName == docName }?.document?.browsable()
                if (doc == null) {
                    tri.util.warning<DocumentQaView>("Unable to find document $docName.")
                } else {
                    browseToBestSnippet(doc, planner.lastResult, hostServices)
                }
            }
        }
    }

    companion object {
        private const val PREF_APP = "promptfx"
        private const val PREF_DOCS_FOLDER = "document-qa.folder"

        private const val PROMPT_PREFIX = "docs-qa"
        private const val JOINER_PREFIX = "snippet-joiners"

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
                    // TODO - support for multiple responses
                    DocumentBrowseToClosestMatch(matches, result.responseEmbeddings[0], hostServices).open()
                }
            }
        }
    }
}