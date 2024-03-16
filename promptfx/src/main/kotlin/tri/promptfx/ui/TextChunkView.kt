package tri.promptfx.ui

import javafx.application.HostServices
import javafx.beans.value.ObservableValue
import javafx.geometry.Pos
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import tornadofx.action
import tornadofx.hyperlink
import tornadofx.text
import tornadofx.tooltip
import tri.ai.embedding.EmbeddingIndex
import tri.promptfx.docs.DocumentBrowseToPage
import tri.util.ui.DocumentUtils

/** View for a [TextChunkViewModel]. */
class TextChunkView(it: TextChunkViewModel, index: ObservableValue<out EmbeddingIndex>, hostServices: HostServices) : HBox() {
    init {
        alignment = Pos.CENTER_LEFT
        it.score?.let { score ->
            text("%.2f".format(score)) {
                style = "-fx-font-weight: bold;"
            }
        }
        it.doc?.let { doc ->
            hyperlink(doc.shortNameWithoutExtension) {
                val thumb = DocumentUtils.documentThumbnail(index.value, doc)
                if (thumb != null) {
                    tooltip { graphic = ImageView(thumb) }
                }
                action { DocumentBrowseToPage(index.value, doc, it.text, hostServices).open() }
            }
        }

        val text = it.text
        val shortText = text.take(50).replace("\n", " ").replace("\r", " ").trim()
        text("$shortText...") {
            tooltip(text) {
                maxWidth = 500.0
                isWrapText = true
            }
        }
    }
}