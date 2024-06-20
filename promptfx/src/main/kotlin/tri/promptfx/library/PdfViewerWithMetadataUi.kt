package tri.promptfx.library

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleIntegerProperty
import javafx.geometry.Orientation
import javafx.scene.control.ButtonBar
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.core.TextCompletion
import tri.ai.text.chunks.TextDoc
import tri.ai.text.chunks.TextDocMetadata
import tri.ai.text.chunks.process.LocalFileManager.extractMetadata
import tri.ai.text.chunks.process.PdfMetadataGuesser
import tri.promptfx.AiProgressView
import tri.promptfx.PromptFxController
import tri.promptfx.library.TextLibraryCollectionUi.Companion.merge
import tri.util.ui.graphic
import tri.util.ui.pdf.PdfViewer

/**
 * UI that shows a PDF viewer alongside the metadata validator UI.
 */
class PdfViewerWithMetadataUi(
    val doc: TextDoc,
    val onClose: (MetadataValidatorModel) -> Unit
): View("PDF Viewer with Metadata Validator") {

    private val pdfFile = doc.pdfFile()!!
    private val controller: PromptFxController by inject()
    private val progress: AiProgressView by inject()
    val metadataModel: MetadataValidatorModel by inject()
    private val metadataGuessPageCount = SimpleIntegerProperty(2)

    init {
        metadataModel.initialProps.setAll(doc.metadata.asGmvPropList(""))
    }

    override val root = borderpane {
        prefWidth = 1200.0
        prefHeight = 800.0
        center = splitpane(Orientation.HORIZONTAL) {
            add(PdfViewer().apply {
                viewModel.documentURIString.value = pdfFile.toURI().toString()
                viewModel.currentPageNumber.value = 0
            })
            borderpane {
                top = vbox(5) {
                    text("Document Metadata") {
                        style = "-fx-font-size: 24"
                    }
                    toolbar {
                        text("Calculate/Extract Metadata:")
                        button("From File", graphic = FontAwesomeIcon.INFO_CIRCLE.graphic) {
                            tooltip("Extract metadata saved with the file (results vary by file type).")
                            action {
                                executeMetadataExtraction()
                            }
                        }
                        text("or")
                        button("Guess", graphic = FontAwesomeIcon.MAGIC.graphic) {
                            tooltip("Attempt to automatically find data using an LLM to extract likely metadata from document text.")
                            disableWhen(progress.activeProperty.or(controller.completionEngine.isNull()))
                            action {
                                executeMetadataGuess(controller.completionEngine.value)
                            }
                        }
                        val ttp = "The number of pages to use when guessing metadata from the document."
                        text("from first") { tooltip(ttp) }
                        spinner(1, 100, 2, 1, true, metadataGuessPageCount) {
                            tooltip(ttp)
                            prefWidth = 50.0
                        }
                        text("pages") { tooltip(ttp) }
                        progressindicator(progress.indicator.progressProperty()) {
                            visibleWhen(this@PdfViewerWithMetadataUi.progress.activeProperty)
                            managedWhen(this@PdfViewerWithMetadataUi.progress.activeProperty)
                            setMinSize(20.0, 20.0)
                        }
                    }
                }
                center = MetadataValidatorUi().root
            }
        }
        bottom = buttonbar {
            paddingAll = 10.0
            button("OK", ButtonBar.ButtonData.OK_DONE) {
                action {
                    close()
                    onClose(metadataModel)
                }
            }
            button("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE) {
                action {
                    close()
                }
            }
        }
    }

    /** Extract metadata for the given document if possible. */
    private fun executeMetadataExtraction() {
        if (pdfFile.exists()) {
            val md = pdfFile.extractMetadata()
            if (md.isNotEmpty()) {
                val metadata = TextDocMetadata("")
                metadata.merge(md)
                metadataModel.merge(metadata.asGmvPropList("File Metadata"))
            }
        }
    }

    /** Execute the metadata guesser on the PDF file on a background thread. */
    private fun executeMetadataGuess(completion: TextCompletion) {
        runAsync {
            val result = runBlocking {
                PdfMetadataGuesser.guessPdfMetadata(completion, pdfFile, metadataGuessPageCount.value) {
                    this@PdfViewerWithMetadataUi.progress.taskStarted(it)
                }
            }
            this@PdfViewerWithMetadataUi.progress.taskCompleted()
            result
        } ui {
            metadataModel.merge(it.asGmvPropList())
        }
    }
}