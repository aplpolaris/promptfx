package tri.promptfx.ui

import javafx.application.HostServices
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.embedding.EmbeddingDocument
import tri.ai.embedding.EmbeddingIndex
import tri.promptfx.docs.DocumentOpenInViewer
import tri.util.ui.DocumentUtils

/**
 * Display a list of documents.
 * Clicking on the document name will open the document in a viewer.
 */
class DocumentListView(docs: ObservableList<EmbeddingDocument>, index: ObservableValue<out EmbeddingIndex>, hostServices: HostServices) : Fragment("Document List") {
    override val root = listview(docs) {
        vgrow = Priority.ALWAYS
        cellFormat {
            graphic = hbox {
                textflow {  }
                alignment = Pos.CENTER_LEFT
                hyperlink(it.shortNameWithoutExtension) {
                    val thumb = DocumentUtils.documentThumbnail(index.value, it)
                    if (thumb != null) {
                        tooltip { graphic = ImageView(thumb) }
                    }
                    action { DocumentOpenInViewer(index.value, it, hostServices).open() }
                }
            }
        }
    }
}