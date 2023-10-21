package tri.promptfx.docs

import javafx.application.HostServices
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.embedding.EmbeddingDocument
import tri.ai.embedding.EmbeddingSectionInDocument
import tri.util.ui.DocumentUtils

/**
 * Display a list of documents.
 * Clicking on the document name will open the document in a viewer.
 */
internal fun EventTarget.docslist(snippets: ObservableList<EmbeddingDocument>, hostServices: HostServices) {
    listview(snippets) {
        vgrow = Priority.ALWAYS
        cellFormat {
            graphic = hbox {
                textflow {  }
                alignment = Pos.CENTER_LEFT
                hyperlink(it.shortNameWithoutExtension) {
                    val thumb = DocumentUtils.documentThumbnail(it)
                    if (thumb != null) {
                        tooltip { graphic = ImageView(thumb) }
                    }
                    action { DocumentOpenInViewer(it, hostServices).open() }
                }
            }
        }
    }
}

/**
 * Display a list of document chunks, along with document thumbnails.
 * Clicking on the document name will open the document in a viewer.
 */
internal fun EventTarget.snippetlist(snippets: ObservableList<EmbeddingSectionInDocument>, hostServices: HostServices) {
    listview(snippets) {
        vgrow = Priority.ALWAYS
        cellFormat {
            graphic = hbox {
                textflow {  }
                alignment = Pos.CENTER_LEFT
                hyperlink(it.doc.shortNameWithoutExtension) {
                    val thumb = DocumentUtils.documentThumbnail(it.doc)
                    if (thumb != null) {
                        tooltip { graphic = ImageView(thumb) }
                    }
                    action { DocumentBrowseToPage(it.doc, it.readText(), hostServices).open() }
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

/**
 * Display a list of matching snippets, along with document thumbnails.
 * Clicking on the document name will open the document in a viewer.
 */
internal fun EventTarget.snippetmatchlist(snippets: ObservableList<SnippetMatch>, hostServices: HostServices) {
    listview(snippets) {
        vgrow = Priority.ALWAYS
        cellFormat {
            graphic = hbox {
                textflow {  }
                alignment = Pos.CENTER_LEFT
                text("%.2f".format(it.score)) {
                    style = "-fx-font-weight: bold;"
                }
                val doc = it.embeddingMatch.document
                hyperlink(doc.shortNameWithoutExtension) {
                    val thumb = DocumentUtils.documentThumbnail(doc)
                    if (thumb != null) {
                        tooltip { graphic = ImageView(thumb) }
                    }
                    action { DocumentBrowseToPage(it.embeddingMatch.document, it.snippetText, hostServices).open() }
                }

                val text = it.snippetText
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
