package tri.promptfx.apps

import javafx.application.HostServices
import javafx.scene.control.Alert
import javafx.stage.Modality
import tornadofx.alert
import tornadofx.find
import tri.ai.embedding.EmbeddingDocument
import tri.ai.embedding.cosineSimilarity
import tri.ai.embedding.findTextInPdf
import java.awt.Desktop
import java.io.File

/** An action to perform when a document link is clicked. */
sealed class DocumentLinkAction {
    abstract fun open()
}

/** Opens the document in the system default application. */
class DocumentOpenInSystem(val document: EmbeddingDocument, val hostServices: HostServices?) : DocumentLinkAction() {
    override fun open() {
        val file1 = File(document.path)
        val fileList = listOf("pdf", "doc", "docx", "txt").map {
            File(file1.parentFile, file1.nameWithoutExtension + ".$it")
        } + file1
        fileList.firstOrNull { it.exists() }?.let {
            val fullPath = it.toURI().toString()
            if (hostServices != null) {
                hostServices.showDocument(fullPath)
            } else {
                Desktop.getDesktop().open(it)
            }
        } ?: run {
            alert(Alert.AlertType.ERROR, "File not found", "Could not find file ${document.path}")
        }
    }
}

/** Opens the document in JavaFx PDF viewer. */
class DocumentOpenInViewer(val document: EmbeddingDocument, val hostServices: HostServices?): DocumentLinkAction() {
    override fun open() =
        when {
            document.file.extension.lowercase() == "pdf" -> openPdf(page = 0)
            else -> DocumentOpenInSystem(document, hostServices).open()
        }

    /** Open a PDF to a given page in the viewer. */
    fun openPdf(page: Int) {
        val viewer = find<PdfViewer>().apply {
            viewModel.documentURIString.value = document.url.toURI().toString()
            viewModel.currentPageNumber.value = page
        }
        if (viewer.root.scene?.window?.isShowing == true) {
            viewer.root.scene?.window?.requestFocus()
        } else {
            viewer.openModal(modality = Modality.NONE, resizable = true)
        }
    }
}

/** Browses to a given snippet within a document. */
class DocumentBrowseToPage(val match: SnippetMatch, val hostServices: HostServices?): DocumentLinkAction() {
    override fun open() {
        val doc = match.embeddingMatch.document
        if (doc.file.extension.lowercase() == "pdf") {
            val page = findTextInPdf(doc.file, match.snippetText)
            DocumentOpenInViewer(doc, hostServices).openPdf(page - 1)
        } else {
            DocumentOpenInSystem(doc, hostServices).open()
        }
    }
}

/** Browses to the closest snippet matching given text embedding in a document. */
class DocumentBrowseToClosestMatch(val matches: List<SnippetMatch>, val textEmbedding: List<Double>?, val hostServices: HostServices?): DocumentLinkAction() {
    override fun open() {
        println("Browsing to the closest of ${matches.size} snippets within this document...")
        val closestSnippet = when {
            matches.size == 1 || textEmbedding == null -> matches.first()
            else -> closestMatchToResponse(matches, textEmbedding)
        }
        DocumentBrowseToPage(closestSnippet, hostServices).open()
    }

    /** Calculates the snippet that was most similar to the generated answer. */
    private fun closestMatchToResponse(snippets: List<SnippetMatch>, embedding: List<Double>) =
        snippets.maxBy { cosineSimilarity(it.snippetEmbedding, embedding) }
}