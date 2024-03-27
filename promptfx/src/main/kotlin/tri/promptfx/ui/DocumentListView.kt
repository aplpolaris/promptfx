package tri.promptfx.ui

import javafx.application.HostServices
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.text.chunks.BrowsableSource
import tri.promptfx.docs.DocumentOpenInViewer
import tri.util.ui.DocumentUtils

/**
 * Display a list of documents.
 * Clicking on the document name will open the document in a viewer.
 */
class DocumentListView(docs: ObservableList<BrowsableSource>, hostServices: HostServices) : Fragment("Document List") {
    override val root = listview(docs) {
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