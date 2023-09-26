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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.application.HostServices
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
import javafx.scene.web.WebView
import javafx.stage.Modality
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.embedding.EmbeddingMatch
import tri.ai.embedding.LocalEmbeddingIndex
import tri.ai.embedding.findTextInPdf
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
import java.lang.UnsupportedOperationException
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
            text("Document Snippets:")
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

        var result: WebView? = null
        outputPane.clear()
        output {
            result = webview {
                fontScale = 1.5
                engine.isJavaScriptEnabled = true
                engine.locationProperty().addListener { _, _, newValue ->
                    try {
                        if (newValue.isNotBlank()) {
                            val url = URL(newValue)
                            val file = File(URLDecoder.decode(url.path, "UTF-8"))
                            Desktop.getDesktop().open(file)
                        }
                    } catch (x: MalformedURLException) {
                        println("Invalid URL: $newValue")
                    }
                }
                vgrow = Priority.ALWAYS
            }
        }
        onCompleted {
            val fr = it.finalResult as List<Node>
            result!!.engine.loadContent(CitationTextProcessor.textToHtml(fr))
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
        completionEngine.instructTask(promptId.get(), question.get(), context, maxTokens.value).map {
            queryChunks to it
        }
    }.task("process-result") {
        CitationTextProcessor.textToText(hostServices, it.first, it.second)
    }.planner

    private suspend fun findRelevantSection(query: String): AiTaskResult<List<EmbeddingMatch>> {
        val matches = embeddingIndex.value!!.findMostSimilar(query, chunksToRetrieve.value)
        return AiTaskResult.result(matches)
    }

    //region DOCUMENT SNIPPET VIEW

    /** Shows information on the list of matches, with document thumbnails. */
    private fun EventTarget.matchlistview(snippets: ObservableList<EmbeddingMatch>) {
        listview(snippets) {
            cellFormat {
                vgrow = Priority.ALWAYS
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

/** Adds citations to provided text. */
private object CitationTextProcessor {

    /** Convert from [Text] and [Hyperlink] to HTML. */
    fun textToHtml(resultText: List<Node>): String {
        val text = StringBuilder("<html>\n")
        resultText.forEach {
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
                is Hyperlink -> text.append("<a href=\"${it.text}\">${it.text}</a>")
                else -> throw UnsupportedOperationException()
            }
        }
        return text.toString()
    }

    fun textToText(hostServices: HostServices, matches: List<EmbeddingMatch>, resultText: String): List<Node> {
        val result = mutableListOf<Node>(Text(resultText))
        val docs = matches.map { it.document }.toSet()
        docs.forEach { doc ->
            result.splitOn(doc.shortName) {
                Hyperlink().apply {
                    text = doc.shortNameWithoutExtension
                    action { browseToDocument(hostServices, doc) }
                }
            }
        }
        result.splitOn("Citations:") {
            Text(it).apply { style = "-fx-font-weight: bold;" }
        }
        result.splitOn("\\[[0-9]+]".toRegex()) {
            Text(it).apply { style = "-fx-fill: #8abccf; -fx-font-weight: bold;" }
        }
        return result
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