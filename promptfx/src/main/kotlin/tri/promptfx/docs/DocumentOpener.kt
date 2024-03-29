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

import javafx.application.HostServices
import javafx.scene.control.Alert
import javafx.stage.Modality
import tornadofx.alert
import tornadofx.find
import tri.ai.embedding.EmbeddingDocument
import tri.ai.embedding.EmbeddingIndex
import tri.ai.embedding.cosineSimilarity
import tri.ai.embedding.findTextInPdf
import java.awt.Desktop

/** An action to perform when a document link is clicked. */
sealed class DocumentOpener {
    abstract fun open()
}

/** Opens the document in the system default application. */
class DocumentOpenInSystem(val index: EmbeddingIndex, val document: EmbeddingDocument, val hostServices: HostServices?) : DocumentOpener() {
    override fun open() {
        index.documentUrl(document)?.let {
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
class DocumentOpenInViewer(val index: EmbeddingIndex, val document: EmbeddingDocument, val hostServices: HostServices?): DocumentOpener() {

    override fun open() {
        val file = index.documentUrl(document) ?: return
        when {
            file.extension.lowercase() == "pdf" -> openPdf(page = 0)
            else -> DocumentOpenInSystem(index, document, hostServices).open()
        }
    }

    /** Open a PDF to a given page in the viewer. */
    fun openPdf(page: Int) {
        val file = index.documentUrl(document) ?: return
        val viewer = find<PdfViewer>().apply {
            viewModel.documentURIString.value = file.toURI().toString()
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
class DocumentBrowseToPage(val index: EmbeddingIndex, val doc: EmbeddingDocument, val text: String, val hostServices: HostServices?): DocumentOpener() {
    override fun open() {
        val file = index.documentUrl(doc) ?: return
        if (file.extension.lowercase() == "pdf") {
            val page = findTextInPdf(file, text)
            DocumentOpenInViewer(index, doc, hostServices).openPdf(page - 1)
        } else {
            DocumentOpenInSystem(index, doc, hostServices).open()
        }
    }
}

/** Browses to the closest snippet matching given text embedding in a document. */
class DocumentBrowseToClosestMatch(val index: EmbeddingIndex, val matches: List<SnippetMatch>, val textEmbedding: List<Double>?, val hostServices: HostServices?): DocumentOpener() {
    override fun open() {
        println("Browsing to the closest of ${matches.size} snippets within this document...")
        val closestSnippet = when {
            matches.size == 1 || textEmbedding == null -> matches.first()
            else -> closestMatchToResponse(matches, textEmbedding)
        }
        DocumentBrowseToPage(index, closestSnippet.embeddingMatch.document, closestSnippet.snippetText, hostServices).open()
    }

    /** Calculates the snippet that was most similar to the generated answer. */
    private fun closestMatchToResponse(snippets: List<SnippetMatch>, embedding: List<Double>) =
        snippets.maxBy { cosineSimilarity(it.snippetEmbedding, embedding) }
}
