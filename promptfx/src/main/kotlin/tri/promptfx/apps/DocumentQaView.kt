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
package tri.promptfx.apps

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.application.HostServices
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Hyperlink
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.scene.web.WebView
import javafx.stage.FileChooser
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.embedding.EmbeddingMatch
import tri.ai.embedding.LocalEmbeddingIndex
import tri.ai.embedding.cosineSimilarity
import tri.ai.openai.instructTask
import tri.ai.pips.AiTaskResult
import tri.ai.pips.aitask
import tri.ai.prompt.AiPromptLibrary
import tri.promptfx.AiPlanTaskView
import tri.promptfx.DocumentUtils.browseToDocument
import tri.promptfx.DocumentUtils.browseToSnippet
import tri.promptfx.DocumentUtils.documentThumbnail
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.graphic
import tri.util.ui.slider
import java.awt.Desktop
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder
import java.nio.file.Files

/** Plugin for the [DocumentQaView]. */
class DocumentQaPlugin : NavigableWorkspaceViewImpl<DocumentQaView>("Text", "Document Q&A", DocumentQaView::class)

/** A view that allows the user to ask a question about a document, and the system will find the most relevant section. */
class DocumentQaView: AiPlanTaskView(
    "Document Q&A",
    "Enter question below to respond based on content of documents in a specified folder.",
) {

    companion object {
        private const val PREF_APP = "promptfx"
        private const val PREF_DOCS_FOLDER = "document-qa.folder"
    }

    private val promptId = SimpleStringProperty("question-answer-docs")
    private val promptIdList = AiPromptLibrary.INSTANCE.prompts.keys.filter { it.startsWith("question-answer") }

    private val promptText = promptId.stringBinding { AiPromptLibrary.lookupPrompt(it!!).template }

    internal val question = SimpleStringProperty("")
    val snippets = observableListOf<EmbeddingMatch>()

    internal val documentFolder = SimpleObjectProperty(File(""))
    private val maxChunkSize = SimpleIntegerProperty(1000)
    private val chunksToRetrieve = SimpleIntegerProperty(10)
    private val minChunkSizeForRelevancy = SimpleIntegerProperty(50)
    private val chunksToSendWithQuery = SimpleIntegerProperty(5)
    private val maxTokens = SimpleIntegerProperty(1000)

    private val embeddingIndex = controller.embeddingService.objectBinding(documentFolder, maxChunkSize) {
        LocalEmbeddingIndex(documentFolder.value, it!!).apply {
            maxChunkSize = this@DocumentQaView.maxChunkSize.value
        }
    }
    private val isScoreResult = SimpleBooleanProperty(true)

    private var lastResult: QuestionAnswerResult? = null
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
                    disableWhen(snippets.sizeProperty.isEqualTo(0))
                    action {
                        val file = chooseFile("Export Document Snippets as JSON", arrayOf(FileChooser.ExtensionFilter("JSON", "*.json")), mode = FileChooserMode.Save, owner = currentWindow)
                        if (file.isNotEmpty()) {
                            runAsync {
                                runBlocking {
                                    ObjectMapper()
                                        .writerWithDefaultPrettyPrinter()
                                        .writeValue(file.first(), lastResult)
                                }
                            }
                        }
                    }
                }
            }
            matchlistview(snippets)
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
                                runBlocking {
                                    embeddingIndex.value!!.reindexAll()
                                }
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
            field("Max tokens") {
                tooltip("Max # of tokens for combined query/response from the question answering engine")
                slider(1..2000, maxTokens)
                label(maxTokens)
            }
        }
        parameters("Prompt Template") {
            tooltip("Templates are defined in prompts.yaml")
            field("Template") {
                combobox(promptId, promptIdList)
            }
            field(null, forceLabelIndent = true) {
                text(promptText).apply {
                    wrappingWidth = 300.0
                    promptText.onChange { tooltip(it) }
                }
            }
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
                }
            }
        }
        onCompleted {
            val fr = it.finalResult as FormattedText
            val html = CitationTextProcessor.textToHtml(fr)
            htmlResult.set(html)
            resultBox.children.clear()
            resultBox.children.addAll(fr.nodes)
        }
    }

    override fun plan() = aitask("calculate-embeddings") {
        runLater { snippets.setAll() }
        findRelevantSection(question.get()).also {
            runLater { snippets.setAll(it.value) }
        }
    }.aitask("question-answer") {
        val queryChunks = it.filter { it.section.length >= minChunkSizeForRelevancy.value }
            .take(chunksToSendWithQuery.value)
        val context = ContextBuilder.constructContextForQuery3(queryChunks)
        val response = completionEngine.instructTask(promptId.get(), question.get(), context, maxTokens.value)
        val responseEmbedding = response.value?.let { calcEmbedding(it) }
        response.map {
            QuestionAnswerResult(
                modelId = controller.completionEngine.value.modelId,
                embeddingId = controller.embeddingService.value.modelId,
                promptId = promptId.value,
                question = question.value,
                questionEmbedding = queryChunks.first().queryEmbedding,
                matches = snippets.map { SnippetMatch(it) },
                response = response.value,
                responseEmbedding = responseEmbedding
            )
        }
    }.task("process-result") {
        if (isScoreResult.value) {
            println("Similarity of question to response: " + it.questionAnswerSimilarity())
        }
        lastResult = it
        CitationTextProcessor.textToText(hostServices, it, browseToPage = isScoreResult.value)
    }.planner

    //region SIMILARITY CALCULATIONS

    /** Finds the most relevant section to the query. */
    private suspend fun findRelevantSection(query: String): AiTaskResult<List<EmbeddingMatch>> {
        val matches = embeddingIndex.value!!.findMostSimilar(query, chunksToRetrieve.value)
        return AiTaskResult.result(matches)
    }

    /** Calculates the embedding for a given response. */
    private suspend fun calcEmbedding(response: String) = if (isScoreResult.value) {
        embeddingIndex.value!!.embeddingService.calculateEmbedding(response)
    } else {
        null
    }

    //endregion

    //region DOCUMENT SNIPPET VIEW

    /** Shows information on the list of matches, with document thumbnails. */
    private fun EventTarget.matchlistview(snippets: ObservableList<EmbeddingMatch>) {
        listview(snippets) {
            vgrow = Priority.ALWAYS
            cellFormat {
                graphic = hbox {
                    textflow {  }
                    alignment = Pos.CENTER_LEFT
                    text("%.2f".format(it.score)) {
                        style = "-fx-font-weight: bold;"
                    }
                    hyperlink(it.document.shortNameWithoutExtension) {
                        val thumb = documentThumbnail(it.document)
                        if (thumb != null) {
                            tooltip { graphic = ImageView(thumb) }
                        }
                        action { browseToSnippet(hostServices, it) }
                    }

                    val text = it.readText()
                    val shortText = text.take(50).replace("\n", " ").replace("\r", " ").trim()
                    text("$shortText...") {
                        tooltip(text) {
                            maxWidth = 500.0
                            isWrapText = true
                        }
                    }
                }
            }
        }
    }

    //endregion
}

//region HELPER OBJECTS

/** Result object. */
data class QuestionAnswerResult(
    val modelId: String,
    val embeddingId: String,
    val promptId: String?,
    val question: String,
    val questionEmbedding: List<Double>,
    val matches: List<SnippetMatch>,
    val response: String?,
    val responseEmbedding: List<Double>?
) {
    override fun toString() = response ?: "No response. Question: $question"

    /** Calculates the similarity between the question and response. */
    internal fun questionAnswerSimilarity() = responseEmbedding?.let { cosineSimilarity(questionEmbedding, it) } ?: 0

    /** Calculates the snippet that was most similar to the generated answer. */
    internal fun closestMatchToResponse(snippets: List<SnippetMatch>) =
        snippets.maxByOrNull { cosineSimilarity(it.snippetEmbedding, responseEmbedding!!) }
}

/** A snippet match that can be serialized. */
data class SnippetMatch(
    @get:JsonIgnore val embeddingMatch: EmbeddingMatch,
    val document: String,
    val snippetStart: Int,
    val snippetEnd: Int,
    val snippetText: String,
    val snippetEmbedding: List<Double>,
    val score: Double
) {
    constructor(match: EmbeddingMatch) : this(
        match,
        match.document.shortName,
        match.section.start,
        match.section.end,
        match.readText(),
        match.section.embedding,
        match.score
    )
}

/** A result that contains plain text and links. */
class FormattedText(val nodes: List<Node>) {
    override fun toString() = nodes.joinToString("") {
        (it as? Text)?.text ?: (it as? Hyperlink)?.text ?: ""
    }
}

/** Adds citations to provided text. */
private object CitationTextProcessor {

    /** Convert the plaintext result to a list of text/hyperlink objects. */
    fun textToText(hostServices: HostServices, qaResult: QuestionAnswerResult, browseToPage: Boolean): FormattedText {
        val result = mutableListOf<Node>(Text(qaResult.response ?: "No response."))
        val docs = qaResult.matches.map { it.document }.toSet()
        docs.forEach { doc ->
            result.splitOn(doc) {
                Hyperlink().apply {
                    text = qaResult.matches.first { it.document == doc }.embeddingMatch.document.shortNameWithoutExtension
                    action { browseToDocument(hostServices, qaResult, doc, browseToPage) }
                }
            }
        }
        result.splitOn("Citations:") {
            Text(it).apply { style = "-fx-font-weight: bold;" }
        }
        result.splitOn("\\[[0-9]+(,\\s*[0-9]+)*]".toRegex()) {
            Text(it).apply { style = "-fx-fill: #8abccf; -fx-font-weight: bold;" }
        }
        return FormattedText(result)
    }

    /** Convert from [Text] and [Hyperlink] to HTML. */
    fun textToHtml(resultText: FormattedText): String {
        val text = StringBuilder("<html>\n")
        resultText.nodes.forEach {
            when (it) {
                is Text -> {
                    val style = it.style
                    val bold = style.contains("-fx-font-weight: bold")
                    val italic = style.contains("-fx-font-style: italic")
                    val color = if (style.contains("-fx-fill:"))
                        style.substringAfter("-fx-fill:").let { it.substringBefore(";", it) }
                    else null
                    val prefix = (if (bold) "<b>" else "") + (if (italic) "<i>" else "") +
                            (if (color != null) "<font color=\"$color\">" else "")
                    val suffix = (if (color != null) "</font>" else "") +
                            (if (italic) "</i>" else "") + (if (bold) "</b>" else "")
                    text.append(prefix + it.text.replace("\n", "<br>") + suffix)
                }
                is Hyperlink ->
                    text.append("<a href=\"${it.text}\">${it.text}</a>")
                else -> throw UnsupportedOperationException()
            }
        }
        return text.toString()
    }

    /** Browses to the document, or to one of the snippets if possible. */
    fun browseToDocument(hostServices: HostServices, qaResult: QuestionAnswerResult, doc: String, browseToPage: Boolean) {
        if (browseToPage) {
            val docSnippets = qaResult.matches.filter { it.document == doc }
            println("Browsing to the closest of ${docSnippets.size} snippets within this document...")
            val closestSnippet = if (docSnippets.size == 1) docSnippets.first() else qaResult.closestMatchToResponse(docSnippets)!!
            browseToSnippet(hostServices, closestSnippet.embeddingMatch)
        } else {
            val sourceDoc = qaResult.matches.first { it.document == doc }.embeddingMatch.document
            browseToDocument(hostServices, sourceDoc)
        }
    }

    /** Splits all text elements on a given search string. */
    private fun MutableList<Node>.splitOn(find: String, replace: (String) -> Node) {
        splitOn(Regex.fromLiteral(find), replace)
    }

    /** Splits all text elements on a given search string. */
    private fun MutableList<Node>.splitOn(find: Regex, replace: (String) -> Node) {
        toList().forEach {
            if (it is Text) {
                val newNodes = it.splitOn(find, replace)
                if (newNodes != listOf(it)) {
                    addAll(indexOf(it), newNodes)
                    remove(it)
                }
            }
        }
    }

    /** Splits the text on a given search string, replacing it using the provided function. */
    private fun Text.splitOn(find: Regex, replace: (String) -> Node): List<Node> {
        val result = mutableListOf<Node>()
        var index = 0
        find.findAll(text).forEach {
            val text0 = text.substring(index, it.range.first)
            if (text0.length > 1) {
                result += Text(text0)
            }
            result += replace(it.value)
            index = it.range.last + 1
        }
        result += Text(text.substring(index))
        return result
    }

}

//endregion

/** Combines multiple text chunks into a single context. */
private object ContextBuilder {

    private fun constructContextForQuery1(matches: List<EmbeddingMatch>): String {
        return matches.joinToString("\n```\n") {
            "Document: ${File(it.document.path).name}\n```\nRelevant Text: ${it.readText()}"
        }
    }

    private fun constructContextForQuery2(matches: List<EmbeddingMatch>): String {
        val docs = matches.groupBy { it.document.shortName }
        return docs.entries.mapIndexed { i, entry ->
            "  - \"\"" + entry.value.joinToString("\n... ") { it.readText().trim() } + "\"\"" +
                    " [${i+1}] ${entry.key}"
        }.joinToString("\n  ")
    }

    internal fun constructContextForQuery3(matches: List<EmbeddingMatch>): String {
        var n = 1
        val docs = matches.groupBy { it.document.shortName }
        return docs.map { (doc, text) ->
            val combinedText = text.joinToString("\n... ") { it.readText().trim() }
            "[[Citation ${n++}]] ${doc}\n\"\"\"\n$combinedText\n\"\"\"\n"
        }.joinToString("\n\n")
    }

}