/*-
 * #%L
 * promptfx-0.1.9-SNAPSHOT
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
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.binding.BooleanBinding
import javafx.beans.property.SimpleFloatProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Pos
import javafx.scene.effect.BlurType
import javafx.scene.effect.InnerShadow
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import tornadofx.*
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI

/**
 * This code is adapted from https://github.com/edvin/tornadofx-samples/blob/master/pdf-viewer/src/main/kotlin/pdf/Main.kt.
 */
class PdfViewer : View("Pdf Viewer") {
    private val pdfModel: PdfViewModel by inject()

    override val root = borderpane {
        top = hbox(spacing = 10) {
            alignment = Pos.CENTER
            paddingAll = 10
            // go to first page
            button("", FontAwesomeIconView(FontAwesomeIcon.ANGLE_DOUBLE_LEFT)) {
                action(pdfModel::firstPage)
                disableWhen(pdfModel.isFirst)
            }
            // previous page
            button("", FontAwesomeIconView(FontAwesomeIcon.ANGLE_LEFT)) {
                action(pdfModel::previousPage)
                disableWhen(pdfModel.isFirst)
            }
            textfield(pdfModel.currentPageNumber + 1)
            label(pdfModel.pageCount)
            // next page
            button("", FontAwesomeIconView(FontAwesomeIcon.ANGLE_RIGHT)) {
                action(pdfModel::nextPage)
                disableWhen(pdfModel.isLast)
            }
            // last page
            button("", FontAwesomeIconView(FontAwesomeIcon.ANGLE_DOUBLE_RIGHT)) {
                action(pdfModel::lastPage)
                disableWhen(pdfModel.isLast)
            }
        }
        center {
            hgrow = Priority.ALWAYS
            vgrow = Priority.ALWAYS
            scrollpane {
                style {
                    padding = box(0.px, 60.px, 0.px, 60.px)
                    backgroundColor += Color.DARKGRAY
                    effect = InnerShadow(BlurType.THREE_PASS_BOX, Color.GRAY, 10.0, 10.0, 10.0, 10.0)
                }
                imageview(pdfModel.currentPage) {
                    minWidthProperty().bind(this@scrollpane.widthProperty())
                }
            }
        }
    }

    fun uriString(uri: String) {
        pdfModel.documentURIString.value = uri
    }

}

class PdfViewModel : ViewModel() {
    var documentURIString = SimpleStringProperty("")
    private var documentURI = SimpleObjectProperty(URI(""))
    private var documentInputStream = SimpleObjectProperty<InputStream>()
    private var document: PDDocument? = null
    private var pdfRenderer: PDFRenderer? = null
    val currentPage = SimpleObjectProperty<Image>(WritableImage(1, 1))
    val currentPageNumber = SimpleIntegerProperty(0)
    val pageCount = SimpleIntegerProperty(0)
    private val scale = SimpleFloatProperty(1.25f)

    init {
        documentInputStream.onChange { input ->
            if (input is InputStream) {
                document = PDDocument.load(input)
                pdfRenderer = PDFRenderer(document)
                pageCount.value = document?.pages?.count
                openPage(0)
            }
        }
        documentURIString.onChange { documentURI.value = URI(it) }
        documentURI.onChange { nuevaUri ->
            val input = when (nuevaUri!!.scheme) {
                "file" -> FileInputStream(nuevaUri.toURL().file)
                else -> null
            }
            documentInputStream.value = input
        }
        currentPageNumber.onChange { openPage(it) }
    }

    private fun openPage(pageCounter: Int) {
        val bim = pdfRenderer?.renderImage(pageCounter, scale.value)//pdfRenderer?.renderImageWithDPI(pageCounter, 300)
        if (bim != null) {
            currentPage.value = SwingFXUtils.toFXImage(bim, null)
        }
    }

    fun firstPage() {
        currentPageNumber.value = 0
    }

    fun previousPage() {
        currentPageNumber.value--
    }

    fun nextPage() {
        currentPageNumber.value++
    }

    fun lastPage() {
        currentPageNumber.value = pageCount.value - 1
    }

    var isFirst: BooleanBinding = currentPageNumber.isEqualTo(0)

    var isLast: BooleanBinding = currentPageNumber.isEqualTo(pageCount - 1)
}
