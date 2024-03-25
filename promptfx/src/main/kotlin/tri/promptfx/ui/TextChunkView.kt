package tri.promptfx.ui

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
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
class TextChunkView(it: TextChunkViewModel, index: ObservableValue<out EmbeddingIndex>?, hostServices: HostServices) : HBox() {
    init {
        spacing = 5.0
        alignment = Pos.CENTER_LEFT
        it.embedding?.let {
            children.add(FontAwesomeIconView(FontAwesomeIcon.MAP_MARKER))
        }
        it.score?.let { score ->
            text("%.2f".format(score)) {
                style = "-fx-font-weight: bold;"
            }
        }
        it.doc?.let { doc ->
            index?.value?.let { embeddingIndex ->
                hyperlink(doc.shortNameWithoutExtension) {
                    val thumb = DocumentUtils.documentThumbnail(embeddingIndex, doc)
                    if (thumb != null) {
                        tooltip { graphic = ImageView(thumb) }
                    }
                    action { DocumentBrowseToPage(embeddingIndex, doc, it.text, hostServices).open() }
                }
            }
        }

        val text = it.text
        val shortText = (if (text.length <= 80) text else "${text.take(80)}...").replace("\n", " ").replace("\r", " ").trim()
        text(shortText) {
            tooltip(text) {
                maxWidth = 500.0
                isWrapText = true
            }
        }
    }
}