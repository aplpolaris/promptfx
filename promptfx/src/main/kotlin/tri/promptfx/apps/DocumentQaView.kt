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
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.scene.web.WebView
import javafx.stage.Modality
import javafx.stage.Window
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist
import tornadofx.*
import tri.ai.embedding.EmbeddingMatch
import tri.ai.embedding.LocalEmbeddingIndex
import tri.ai.openai.instructTask
import tri.ai.pips.AiTaskResult
import tri.ai.pips.aitask
import tri.ai.prompt.AiPromptLibrary
import tri.promptfx.AiPlanTaskView
import tri.promptfx.DocumentUtils.browseToDocument
import tri.promptfx.DocumentUtils.documentThumbnail
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.graphic
import tri.util.ui.slider
import java.awt.Desktop
import java.io.File
import java.io.IOException
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
            result!!.engine.loadContent(it.finalResult.toString())
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
        val context = constructContextForQuery3(queryChunks)
        completionEngine.instructTask(promptId.get(), question.get(), context, maxTokens.value).map {
            queryChunks to it
        }
    }.task("process-result") {
        var text = it.second
        val docs = it.first.map { it.document }.toSet()
        docs.forEach {
            text = text.replace(it.shortName, "<a href=\"${it.url}\">${it.shortNameWithoutExtension}</a>")
        }
        // replace "Citations" and all numbers in brackets [1] and similar with bold purple
        text = text.replace("Citations:", "<b>Citations:</b>")
        text = "\\[[0-9]+]".toRegex().replace(text) {
            // make it bold and purple
            "<font color=\"#800080\"><b>${it.value}</b></font>"
        }
        // insert line breaks
        "<html>\n" + text.replace("\n", "\n<br>\n")
    }.planner

    private suspend fun findRelevantSection(query: String): AiTaskResult<List<EmbeddingMatch>> {
        val matches = embeddingIndex.value!!.findMostSimilar(query, chunksToRetrieve.value)
        return AiTaskResult.result(matches)
    }

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

    private fun constructContextForQuery3(matches: List<EmbeddingMatch>): String {
        var n = 1
        val docs = matches.groupBy { it.document.shortName }
        return docs.map { (doc, text) ->
            val combinedText = text.joinToString("\n... ") { it.readText().trim() }
            "[[Citation ${n++}]] ${doc}\n\"\"\"\n$combinedText\n\"\"\"\n"
        }.joinToString("\n\n")
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
                        action {
                            val page = findTextInPdf(it.document.file, it.readText().take(20))
                            browseToDocument(it.document)
                            find<PdfViewer>().apply {
                                uriString(it.document.url.toURI().toString())
                                if (page != -1) {
                                    println("switching to page $page")
                                    find<PdfViewModel>().apply {
                                        currentPageNumber.value = page - 1
                                    }
                                }
                            }.openModal(modality = Modality.NONE, resizable = false)
                        }
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

private fun findTextInPdf(pdfFile: File, searchText: String): Int {
    PDDocument.load(pdfFile).use { document ->
        val stripper = PDFTextStripper()
        val totalPages = document.numberOfPages
        for (page in 1..totalPages) {
            stripper.startPage = page
            stripper.endPage = page
            val textOnPage = stripper.getText(document)
            if (textOnPage.contains(searchText))
                return page
        }
    }
    return -1
}

//region WEBSITE CRAWL

/** Dialog for choosing web scraper settings. */
class TextCrawlDialog: Fragment("Web Crawler Settings") {

    val folder: SimpleObjectProperty<File> by param()

    private val url = SimpleStringProperty("http://")

    override val root = vbox {
        form {
            fieldset("Crawl Settings") {
                field("URL to Scrape") {
                    textfield(url) {
                        tooltip("Enter URL starting with http:// or https://")
                    }
                    button("", FontAwesomeIcon.GLOBE.graphic) {
                        disableWhen(url.isEmpty)
                        action { hostServices.showDocument(url.get()) }
                    }
                }
                field("Target Folder") {
                    hyperlink(folder.stringBinding {
                        val path = it!!.absolutePath
                        if (path.length > 25) {
                            "..." + path.substring(path.length - 24)
                        } else {
                            path
                        }
                    }) {
                        action {
                            Files.createDirectories(folder.get().toPath())
                            Desktop.getDesktop().open(folder.get())
                        }
                    }
                    button("", FontAwesomeIcon.FOLDER_OPEN.graphic) {
                        action { folder.chooseFolder(currentStage) }
                    }
                }
            }
        }
        buttonbar {
            padding = Insets(10.0)
            spacing = 10.0
            button("Crawl") {
                action {
                    crawlWebsite(url.value, depth = 1, targetFolder = folder.value)
                    close()
                }
            }
        }
    }
}

private fun SimpleObjectProperty<File>.chooseFolder(owner: Window?) {
    chooseDirectory(
        title = "Select Document Folder",
        initialDirectory = value,
        owner = owner
    )?.let {
        set(it)
    }
}

private fun crawlWebsite(url: String, depth: Int = 0, targetFolder: File, scraped: MutableSet<String> = mutableSetOf()) {
    if (url.isBlank() || url in scraped)
        return
    runAsync {
        println("Scraping text and links from $url...")
        try {
            val doc = Jsoup.connect(url).get()
            val docNode = doc.select("article").firstOrNull() ?: doc.body()
            val nodeHtml = docNode.apply {
                select("br").before("\\n")
                select("p").before("\\n")
            }.html().replace("\\n", "\n")
            val text = Jsoup.clean(nodeHtml, "", Safelist.none(),
                Document.OutputSettings().apply { prettyPrint(false) }
            )
            if (text.isNotEmpty()) {
                val title = doc.title().replace("[^a-zA-Z0-9.-]".toRegex(), "_")
                if (title.length > 2) // require minimum length to avoid saving off blank links
                    File(targetFolder, "$title.txt").writeText(text)
                scraped.add(url)
            }
            if (depth > 0) {
                val links = docNode.select("a[href]").map { it.absUrl("href") }.toSet().take(100)
                links
                    .filter { it.startsWith("http") }
                    .forEach { crawlWebsite(it, depth - 1, targetFolder, scraped) }
            }
        } catch (x: IOException) {
            println("  ... failed to retrieve URL due to $x")
        }
    }
}

//endregion
