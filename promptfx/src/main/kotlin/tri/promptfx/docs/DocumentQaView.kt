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

import com.fasterxml.jackson.databind.ObjectMapper
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.application.HostServices
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.input.DataFormat
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.text.TextFlow
import javafx.stage.FileChooser
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.core.TextPlugin
import tri.ai.embedding.EmbeddingDocument
import tri.ai.embedding.EmbeddingIndex
import tri.ai.embedding.LocalEmbeddingIndex
import tri.ai.prompt.AiPromptLibrary
import tri.promptfx.AiPlanTaskView
import tri.promptfx.ui.promptfield
import tri.util.ui.*
import java.awt.Desktop
import java.io.File
import java.nio.file.Files

/** Plugin for the [DocumentQaView]. */
class DocumentQaPlugin : NavigableWorkspaceViewImpl<DocumentQaView>("Documents", "Document Q&A", DocumentQaView::class)

/** A view that allows the user to ask a question about a document, and the system will find the most relevant section. */
class DocumentQaView: AiPlanTaskView(
    "Document Q&A",
    "Enter question below to respond based on content of documents in a specified folder.",
) {

    private val promptId = SimpleStringProperty("$PROMPT_PREFIX-docs")
    private val promptText = promptId.stringBinding { AiPromptLibrary.lookupPrompt(it!!).template }

    private val joinerId = SimpleStringProperty("$JOINER_PREFIX-citations")
    private val joinerText = joinerId.stringBinding { AiPromptLibrary.lookupPrompt(it!!).template }

    val question = SimpleStringProperty("")

    val documentFolder = SimpleObjectProperty(File(""))
    private val maxChunkSize = SimpleIntegerProperty(1000)
    private val chunksToRetrieve = SimpleIntegerProperty(10)
    private val minChunkSizeForRelevancy = SimpleIntegerProperty(50)
    private val chunksToSendWithQuery = SimpleIntegerProperty(5)

    val planner = DocumentQaPlanner().apply {
        embeddingIndex = controller.embeddingService.objectBinding(documentFolder, maxChunkSize) {
            LocalEmbeddingIndex(documentFolder.value, it!!).apply {
                maxChunkSize = this@DocumentQaView.maxChunkSize.value
            }
        }
    }
    val snippets
        get() = planner.snippets

    private val htmlResult = SimpleStringProperty("")

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
                    action {
                        val file = chooseFile("Export Document Snippets as JSON", arrayOf(FileChooser.ExtensionFilter("JSON", "*.json")), mode = FileChooserMode.Save, owner = currentWindow)
                        if (file.isNotEmpty()) {
                            runAsync {
                                runBlocking {
                                    ObjectMapper()
                                        .writerWithDefaultPrettyPrinter()
                                        .writeValue(file.first(), planner.lastResult)
                                }
                            }
                        }
                    }
                }
            }
            snippetmatchlist(planner.embeddingIndex, planner.snippets, hostServices)
        }
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
                                runBlocking { planner.reindexAllDocuments() }
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
        parameters("Document Snippet Matching and Query") {
            field("# Matches") {
                tooltip("Number of matching snippets to retrieve from the document database")
                slider(1..50, chunksToRetrieve)
                label(chunksToRetrieve)
            }
            field("Minimum length") {
                tooltip("Snippets with a character count below this limit will be ignored")
                slider(1..1000, minChunkSizeForRelevancy)
                label(minChunkSizeForRelevancy)
            }
            field("Query snippets") {
                tooltip("Number of matching snippets to send to the question answering engine")
                slider(1..20, chunksToSendWithQuery)
                label(chunksToSendWithQuery)
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
        parameters("Prompt Template") {
            tooltip("Loads from prompts.yaml with prefix $PROMPT_PREFIX and $JOINER_PREFIX")
            promptfield("Template", promptId, AiPromptLibrary.withPrefix(PROMPT_PREFIX), promptText, workspace)
            promptfield("Snippet Joiner", joinerId, AiPromptLibrary.withPrefix(JOINER_PREFIX), joinerText, workspace)
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

                    contextmenu {
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
            val fr = it.finalResult as FormattedText
            htmlResult.set(fr.toHtml())
            resultBox.children.clear()
            resultBox.children.addAll(fr.toFxNodes())
        }
    }

    override fun plan() = planner.plan(
        question = question.value,
        promptId = promptId.value,
        embeddingService = controller.embeddingService.value,
        chunksToRetrieve = chunksToRetrieve.value,
        minChunkSize = minChunkSizeForRelevancy.value,
        contextStrategy = GroupingTemplateJoiner(joinerId.value),
        contextChunks = chunksToSendWithQuery.value,
        completionEngine = controller.completionEngine.value,
        maxTokens = common.maxTokens.value,
        tempParameters = common
    )

    // override the user input with post-processing for hyperlinks
    override suspend fun processUserInput() =
        super.processUserInput().also {
            (it.finalResult as? FormattedText)?.hyperlinkOp = { docName ->
                val doc = snippets.firstOrNull { it.document == docName }?.embeddingMatch?.document
                if (doc == null) {
                    println("Unable to find document $docName in snippets.")
                } else {
                    browseToBestSnippet(planner.embeddingIndex.value!!, doc, planner.lastResult, hostServices)
                }
            }
        }

    companion object {
        private const val PREF_APP = "promptfx"
        private const val PREF_DOCS_FOLDER = "document-qa.folder"

        private const val PROMPT_PREFIX = "question-answer"
        private const val JOINER_PREFIX = "snippet-joiner"

        internal fun browseToBestSnippet(index: EmbeddingIndex, doc: EmbeddingDocument, result: QuestionAnswerResult?, hostServices: HostServices) {
            if (result == null) {
                println("Browsing to first page: ${doc.shortNameWithoutExtension}")
                DocumentOpenInViewer(index, doc, hostServices).open()
            } else {
                println("Browsing to best snippet: ${doc.shortNameWithoutExtension}")
                val matches = result.matches.filter { it.matchesDocument(doc.shortNameWithoutExtension) }
                if (matches.size == 1) {
                    println("Browsing to only match")
                    val match = matches.first()
                    DocumentBrowseToPage(index, match.embeddingMatch.document, match.snippetText, hostServices).open()
                } else {
                    println("Browsing to closest match")
                    DocumentBrowseToClosestMatch(index, matches, result.responseEmbedding, hostServices).open()
                }
            }
        }
    }
}
