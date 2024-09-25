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
package tri.promptfx.ui.docs

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
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
import tri.util.ui.graphic

/** View list of documents with controls. */
class TextDocListUi : Fragment() {

    private val model by inject<TextLibraryViewModel>()

    private val docList = model.docList
    private val docSelection = model.docSelection

    private lateinit var docListView: ListView<TextDoc>

    override val root = vbox {
        vgrow = Priority.SOMETIMES
        toolbar {
            text("Documents in Selected Collection(s)")
        }
        docListView = listview(docList) {
            vgrow = Priority.ALWAYS
            bindSelectionBidirectional(docSelection)
            cellFormat { doc ->
                val browsable = doc.browsable()
                graphic = hbox(5, Pos.CENTER_LEFT) {
                    if (browsable != null) {
                        hyperlink(browsable.shortNameWithoutExtension, graphic = browsable.icon()) {
                            val thumb = DocumentUtils.documentThumbnail(browsable, DocumentListView.DOC_THUMBNAIL_SIZE)
                            if (thumb != null) {
                                tooltip { graphic = ImageView(thumb) }
                            }
                            action { DocumentOpenInViewer(browsable, hostServices).open() }
                        }
                    } else {
                        text(doc.metadata.id.substringAfterLast("/")) {
                            graphic = FontAwesomeIcon.FILE_TEXT_ALT.graphic
                        }
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
