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
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.core.TextChatMessage
import tri.ai.core.TextCompletion
import tri.ai.embedding.*
import tri.ai.pips.AiPipelineExecutor
import tri.ai.pips.AiPipelineResult
import tri.ai.pips.AiTaskList
import tri.ai.pips.IgnoreMonitor
import tri.ai.prompt.AiPrompt
import tri.ai.prompt.AiPromptLibrary
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.text.chunks.BrowsableSource
import tri.ai.text.chunks.GroupingTemplateJoiner
import tri.ai.text.chunks.TextLibrary
import tri.ai.text.docs.DocumentQaDriver
import tri.ai.text.docs.DocumentQaPlanner
import tri.ai.text.docs.QuestionAnswerResult
import tri.promptfx.AiPlanTaskView
import tri.promptfx.TextLibraryReceiver
import tri.promptfx.library.TextLibraryInfo
import tri.promptfx.ui.FormattedPromptResultArea
import tri.ai.text.docs.FormattedPromptTraceResult
import tri.promptfx.PromptFxModels
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
import java.io.FileFilter

/** Plugin for the [DocumentQaView]. */
class DocumentQaPlugin : NavigableWorkspaceViewImpl<DocumentQaView>("Documents", "Document Q&A", WorkspaceViewAffordance.INPUT_AND_COLLECTION, DocumentQaView::class)

/** A view that allows the user to ask a question about a document, and the system will find the most relevant section. */
class DocumentQaView: AiPlanTaskView(
    "Document Q&A",
    "Enter question below to respond based on content of documents in a specified folder.",
), TextLibraryReceiver {

    private val viewScope = Scope(workspace)
    private val prompt = PromptSelectionModel("$PROMPT_PREFIX-docs")
    private val joinerPrompt = PromptSelectionModel("$JOINER_PREFIX-citations")

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
    private val historySize = SimpleIntegerProperty(4)

    val planner = DocumentQaPlannerFx().apply {
        documentLibrary = this@DocumentQaView.documentLibrary
        embeddingIndex = controller.embeddingService.objectBinding(documentFolder, maxChunkSize) {
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
                slider(1..10, historySize)
                label(historySize)
            }
        }
        addDefaultTextCompletionParameters(common)
        parameters("Prompt Template") {
            tooltip("Loads from prompts.yaml with prefix $PROMPT_PREFIX and $JOINER_PREFIX")
            promptfield("Template", prompt, AiPromptLibrary.withPrefix(PROMPT_PREFIX), workspace)
            promptfield("Snippet Joiner", joinerPrompt, AiPromptLibrary.withPrefix(JOINER_PREFIX), workspace)
        }

        val resultBox = FormattedPromptResultArea()
        outputPane.clear()
        outputPane.add(resultBox)

        onCompleted {
            resultBox.setFinalResult(it.finalResult)
        }
    }

    /** Executes task on a background thread and updates progress info. */
    override fun runTask(op: suspend () -> AiPipelineResult<*>) {
        val questionInput = question.value
        val questions = if (singleInput.value) listOf(questionInput) else questionInput.split("\n").map { it.trim() }
            .filter { it.isNotBlank() }
        if (questions.isEmpty()) {
            error("No questions found.")
            return
        }
        questions.forEach {
            question.set(it)
            super.runTask { AiPipelineExecutor.execute(questionTaskList(it).planner.plan(), progress) }
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
            completionEngine = controller.completionEngine.value,
            maxTokens = common.maxTokens.value,
            temp = common.temp.value,
            numResponses = common.numResponses.value
        )

    //region ACTIONS

    override fun loadTextLibrary(library: TextLibraryInfo) {
        documentLibrary.set(library.library)
    }

    //endregion

    // override the user input with post-processing for hyperlinks
    override suspend fun processUserInput() =
        super.processUserInput().also {
            when (val final = it.finalResult) {
                is FormattedPromptTraceResult -> final.formattedOutputs.forEach {
                    it.hyperlinkOp = { docName ->
                        val doc = snippets.firstOrNull { it.shortDocName == docName }?.document?.browsable()
                        if (doc == null) {
                            tri.util.warning<DocumentQaView>("Unable to find document $docName in snippets.")
                        } else {
                            browseToBestSnippet(doc, planner.lastResult, hostServices)
                        }
                    }
                }
                is AiPromptTrace<*> -> {
                    // expected when there is an error
                }
                else -> throw IllegalStateException("Unexpected result type: $final")
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
                    // TODO - support for multiple responses
                    DocumentBrowseToClosestMatch(matches, result.responseEmbeddings[0], hostServices).open()
                }
            }
        }
    }
}

/** JavaFx component for managing documents. */
class DocumentQaPlannerFx {
    /** A document library to use for chunks, if available. */
    var documentLibrary = SimpleObjectProperty<TextLibrary>(null)
    /** The embedding index. */
    var embeddingIndex: ObservableValue<out EmbeddingIndex?> = SimpleObjectProperty(NoOpEmbeddingIndex)
    /** The retrieved relevant snippets. */
    val snippets = observableListOf<EmbeddingMatch>()
    /** The most recent result of the QA task. */
    var lastResult: QuestionAnswerResult? = null
    /** The chat history. */
    var chatHistory = observableListOf<TextChatMessage>()
    /** The size of the chat history. */
    var historySize = SimpleIntegerProperty(4)

    /** Reindexes all documents in the current [EmbeddingIndex] (if applicable). */
    suspend fun reindexAllDocuments() {
        (embeddingIndex.value as? LocalFolderEmbeddingIndex)?.reindexAll()
    }

    fun taskList(
        question: String,
        prompt: AiPrompt?,
        chunksToRetrieve: Int?,
        minChunkSize: Int?,
        contextStrategy: GroupingTemplateJoiner,
        contextChunks: Int?,
        completionEngine: TextCompletion?,
        maxTokens: Int?,
        temp: Double?,
        numResponses: Int?
    ): AiTaskList<String> {
        val p = DocumentQaPlanner(embeddingIndex.value!!, completionEngine!!, chatHistory, historySize.value)
        return p.plan(
            question = question,
            prompt = prompt!!,
            chunksToRetrieve = chunksToRetrieve!!,
            minChunkSize = minChunkSize!!,
            contextStrategy = contextStrategy,
            contextChunks = contextChunks!!,
            maxTokens = maxTokens!!,
            temp = temp!!,
            numResponses = numResponses!!,
            snippetCallback = { runLater { snippets.setAll(it) } }
        )
    }
}

/** Document Q&A driver that leverages [DocumentQaView]. */
class DocumentQaViewDriver(val view: DocumentQaView) : DocumentQaDriver {

    override val folders
        get() = view.documentFolder.value.parentFile
            .listFiles(FileFilter { it.isDirectory })!!
            .map { it.name }
    override var folder: String
        get() = view.documentFolder.value.name
        set(value) {
            val folderFile = File(view.documentFolder.value.parentFile, value)
            if (folderFile.exists())
                view.documentFolder.set(folderFile)
        }
    override var completionModel: String
        get() = view.controller.completionEngine.value.modelId
        set(value) {
            view.controller.completionEngine.set(
                PromptFxModels.policy.textCompletionModels().find { it.modelId == value }!!
            )
        }
    override var embeddingModel: String
        get() = view.controller.embeddingService.value.modelId
        set(value) {
            view.controller.embeddingService.set(
                PromptFxModels.policy.embeddingModels().find { it.modelId == value }!!
            )
        }
    override var temp: Double
        get() = view.common.temp.value
        set(value) {
            view.common.temp.set(value)
        }
    override var maxTokens: Int
        get() = view.common.maxTokens.value
        set(value) {
            view.common.maxTokens.set(value)
        }

    override fun initialize() {
        Platform.startup { }
    }

    override fun close() {
        Platform.exit()
    }

    override suspend fun answerQuestion(input: String): AiPipelineResult<String> {
        view.question.set(input)
        return AiPipelineExecutor.execute(view.plan().plan(), IgnoreMonitor) as AiPipelineResult<String>
    }

}
