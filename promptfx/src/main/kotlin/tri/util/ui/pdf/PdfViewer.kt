/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
package tri.util.ui.pdf

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.binding.Bindings
import javafx.beans.binding.BooleanBinding
import javafx.beans.property.*
import javafx.embed.swing.SwingFXUtils
import javafx.scene.control.ScrollPane
import javafx.scene.effect.BlurType
import javafx.scene.effect.DropShadow
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.stage.Screen
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.poi.util.IOUtils
import tornadofx.*
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI
import java.net.URLDecoder

/**
 * This code is adapted from https://github.com/edvin/tornadofx-samples/blob/master/pdf-viewer/src/main/kotlin/pdf/Main.kt.
 */
class PdfViewer : Fragment("") {

    val viewModel = PdfViewModel()

    init {
        titleProperty.bind(viewModel.documentURIString)
    }

    override val root = borderpane {
        hgrow = Priority.ALWAYS
        vgrow = Priority.ALWAYS

        top = toolbar {
            spacer()
            button("", FontAwesomeIconView(FontAwesomeIcon.ANGLE_DOUBLE_LEFT)) {
                tooltip("Go to first page")
                action { viewModel.currentPageNumber.value = 0 }
                disableWhen(viewModel.isFirst)
            }
            button("", FontAwesomeIconView(FontAwesomeIcon.ANGLE_LEFT)) {
                tooltip("Go to previous page")
                action { viewModel.currentPageNumber.value-- }
                disableWhen(viewModel.isFirst)
            }
            textfield("") {
                viewModel.currentPageNumber.onChange { text = (it + 1).toString() }
                action { viewModel.currentPageNumber.value = text.toInt() - 1 }
                prefColumnCount = 3
            }
            label(Bindings.createStringBinding({ "/ " + viewModel.pageCount.value }, viewModel.pageCount)) {
                style { fontSize = 12.px }
            }
            button("", FontAwesomeIconView(FontAwesomeIcon.ANGLE_RIGHT)) {
                tooltip("Go to next page")
                action { viewModel.currentPageNumber.value++ }
                disableWhen(viewModel.isLast)
            }
            button("", FontAwesomeIconView(FontAwesomeIcon.ANGLE_DOUBLE_RIGHT)) {
                tooltip("Go to last page")
                action { viewModel.currentPageNumber.value = viewModel.pageCount.value - 1 }
                disableWhen(viewModel.isLast)
            }
            spacer()
            button("", FontAwesomeIconView(FontAwesomeIcon.FILE_PDF_ALT)) {
                tooltip("Open in system")
                action { hostServices.showDocument(viewModel.documentURIString.value) }
            }
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
                    imageview(viewModel.currentPage) {
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

        // set pref height to 75% of the height of the monitor on which the current stage is showing
        val curScreen = Screen.getScreensForRectangle(primaryStage.x, primaryStage.y, 1.0, 1.0).firstOrNull()
            ?: Screen.getPrimary()
        prefHeight = curScreen.bounds.height * 0.75
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
            try {
                document?.close()
            } catch (rethrown: RuntimeException) {
                throw rethrown
            } catch (_: Exception) { }
            if (input is InputStream) {
                val bytes = IOUtils.toByteArray(input)
                document = Loader.loadPDF(bytes)
                pdfRenderer = PDFRenderer(document)
                pageCount.value = document?.pages?.count
                document?.getPage(0)?.let { setScaleFor(it) }
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
        currentPageNumber.addListener { _, old, nue ->
            try {
                openPage(nue.toInt())
            } catch (x: IndexOutOfBoundsException) {
                currentPageNumber.set(old.toInt())
            }
        }
    }

    private fun setScaleFor(page: PDPage) {
        val box = page.mediaBox
        if (box != null) {
            val width = box.width
            val height = box.height
            val screen = Screen.getPrimary()
            val screenWidth = screen.bounds.width.toFloat() * .75f
            val screenHeight = screen.bounds.height.toFloat() * .75f
            val scaleWidth = screenWidth / width
            val scaleHeight = screenHeight / height
            scale.value = if (scaleWidth < scaleHeight) scaleWidth else scaleHeight
        }
    }

    private fun openPage(pageCounter: Int) {
        currentPageNumber.value = pageCounter
        val bim = pdfRenderer?.renderImage(pageCounter, scale.value)
        if (bim != null) {
            currentPage.value = SwingFXUtils.toFXImage(bim, null)
        }
    }

}
