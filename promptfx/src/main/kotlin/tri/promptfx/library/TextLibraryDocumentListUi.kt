package tri.promptfx.library

import javafx.beans.binding.Bindings
import javafx.geometry.Pos
import javafx.scene.control.ListView
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.stage.Modality
import tornadofx.*
import tri.ai.text.chunks.TextDoc
import tri.promptfx.docs.DocumentOpenInViewer
import tri.promptfx.ui.DocumentListView
import tri.promptfx.ui.DocumentListView.Companion.icon
import tri.util.ui.DocumentUtils
import tri.util.ui.bindSelectionBidirectional

/** View for managing text collections and documents. */
class TextLibraryDocumentListUi : Fragment() {

    private val model by inject<TextLibraryViewModel>()

    private val docList = model.docList
    private val docSelection = model.docSelection

    private lateinit var docListView: ListView<TextDoc>

    override val root = vbox {
        toolbar {
            text("Documents in Selected Collection(s)")
        }
        docListView = listview(docList) {
            vgrow = Priority.ALWAYS
            bindSelectionBidirectional(docSelection)
            cellFormat { doc ->
                val browsable = doc.browsable()!!
                graphic = hbox(5, Pos.CENTER_LEFT) {
                    hyperlink(browsable.shortNameWithoutExtension, graphic = browsable.icon()) {
                        val thumb = DocumentUtils.documentThumbnail(browsable, DocumentListView.DOC_THUMBNAIL_SIZE)
                        if (thumb != null) {
                            tooltip { graphic = ImageView(thumb) }
                        }
                        action { DocumentOpenInViewer(browsable, hostServices).open() }
                    }
                    text(model.savedStatusProperty(doc)) {
                        style = "-fx-font-style: italic; -fx-text-fill: light-gray"
                    }
                }
                lazyContextmenu {
                    item("Open metadata viewer/editor...") {
                        isDisable = doc.pdfFile() == null
                        action { openMetadataViewer(doc) }
                    }
                    separator()
                    item("Remove selected document(s) from collection") {
                        enableWhen(Bindings.isNotEmpty(docSelection))
                        action {
                            model.removeSelectedDocuments()
                        }
                    }
                }
            }
        }
    }

    //region USER ACTIONS

    private fun openMetadataViewer(doc: TextDoc) {
        PdfViewerWithMetadataUi(doc) {
            model.updateMetadata(doc, it.savedValues(), isSelect = true)
        }.openModal(
            modality = Modality.NONE,
            block = false,
            resizable = true
        )
    }

    //endregion

}