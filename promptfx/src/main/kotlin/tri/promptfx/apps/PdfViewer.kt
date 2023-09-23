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
import javafx.beans.binding.Bindings
import javafx.beans.binding.BooleanBinding
import javafx.beans.property.*
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Pos
import javafx.scene.control.ScrollPane
import javafx.scene.effect.BlurType
import javafx.scene.effect.DropShadow
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import okhttp3.internal.closeQuietly
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import tornadofx.*
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI
import java.net.URLDecoder

/**
 * This code is adapted from https://github.com/edvin/tornadofx-samples/blob/master/pdf-viewer/src/main/kotlin/pdf/Main.kt.
 */
class PdfViewer : View("") {
    private val pdfModel: PdfViewModel by inject()
    private val zoomProperty = object : SimpleDoubleProperty(1.0) {
        private val minZoom = 0.1
        private val maxZoom = 3.0
        override fun set(p0: Double) {
            super.set(p0.coerceIn(minZoom..maxZoom))
        }
        override fun setValue(p0: Number?) {
            super.setValue(p0?.toDouble()?.coerceIn(minZoom..maxZoom))
        }
    }
    init {
        titleProperty.bind(pdfModel.documentURIString)
        pdfModel.documentURIString.onChange { zoomProperty.set(1.0) }
    }

    override val root = borderpane {
        hgrow = Priority.ALWAYS
        vgrow = Priority.ALWAYS

        top = hbox(spacing = 10) {
            alignment = Pos.CENTER
            paddingAll = 10
            button("", FontAwesomeIconView(FontAwesomeIcon.ANGLE_DOUBLE_LEFT)) {
                action { pdfModel.currentPageNumber.value = 0 }
                disableWhen(pdfModel.isFirst)
            }
            button("", FontAwesomeIconView(FontAwesomeIcon.ANGLE_LEFT)) {
                action { pdfModel.currentPageNumber.value-- }
                disableWhen(pdfModel.isFirst)
            }
            textfield(pdfModel.currentPageNumber + 1) {
                action { pdfModel.currentPageNumber.value = text.toInt() - 1 }
                prefColumnCount = 3
            }
            label(Bindings.createStringBinding({ "/ " + pdfModel.pageCount.value }, pdfModel.pageCount)) {
                style { fontSize = 12.px }
            }
            button("", FontAwesomeIconView(FontAwesomeIcon.ANGLE_RIGHT)) {
                action { pdfModel.currentPageNumber.value++ }
                disableWhen(pdfModel.isLast)
            }
            button("", FontAwesomeIconView(FontAwesomeIcon.ANGLE_DOUBLE_RIGHT)) {
                action { pdfModel.currentPageNumber.value = pdfModel.pageCount.value - 1 }
                disableWhen(pdfModel.isLast)
            }
//            button("", FontAwesomeIconView(FontAwesomeIcon.SEARCH_MINUS)) {
//                action { zoomProperty /= 1.1 }
//            }
//            button("", FontAwesomeIconView(FontAwesomeIcon.SEARCH_PLUS)) {
//                action { zoomProperty *= 1.1 }
//            }
//            button("Reset Zoom", FontAwesomeIconView(FontAwesomeIcon.SEARCH)) {
//                action { zoomProperty.set(1.0) }
//            }
        }
        center {
            scrollpane {
                hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
                vbarPolicy = ScrollPane.ScrollBarPolicy.ALWAYS
                isFitToHeight = true
                style {
                    padding = box(0.px, 40.px, 0.px, 40.px)
                    backgroundColor += Color.DARKGRAY
                }
                stackpane {
                    imageview(pdfModel.currentPage) {
                        style {
                            borderColor += box(Color.BLACK)
                            effect = DropShadow(BlurType.THREE_PASS_BOX, Color.GRAY, 10.0, 0.0, 0.0, 0.0)
                        }
                        fitWidthProperty().bind(this@scrollpane.widthProperty() - 80)
                        isPreserveRatio = true
                        isSmooth = true
                        isCache = true
                    }
                }
            }
        }
    }

    fun uriString(uri: String) {
        pdfModel.documentURIString.value = uri
    }

}

/** Model for [PdfViewer]. */
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

    var isFirst: BooleanBinding = currentPageNumber.isEqualTo(0)
    var isLast: BooleanBinding = currentPageNumber.isEqualTo(pageCount - 1)

    init {
        documentInputStream.onChange { input ->
            document?.closeQuietly()
            if (input is InputStream) {
                document = PDDocument.load(input)
                pdfRenderer = PDFRenderer(document)
                pageCount.value = document?.pages?.count
                openPage(0)
            }
        }
        documentURIString.onChange { documentURI.value = URI(it) }
        documentURI.onChange {
            val input = when (it!!.scheme) {
                "file" -> FileInputStream(URLDecoder.decode(it.path, "UTF-8"))
                else -> null
            }
            documentInputStream.value = input
        }
        currentPageNumber.onChange { openPage(it) }
    }

    private fun openPage(pageCounter: Int) {
        val bim = pdfRenderer?.renderImage(pageCounter, scale.value)
        if (bim != null) {
            currentPage.value = SwingFXUtils.toFXImage(bim, null)
        }
    }

}
