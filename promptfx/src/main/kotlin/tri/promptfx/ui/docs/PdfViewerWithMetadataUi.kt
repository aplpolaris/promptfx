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
import javafx.beans.property.SimpleIntegerProperty
import javafx.geometry.Orientation
import javafx.scene.control.Alert
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.core.TextCompletion
import tri.ai.text.chunks.TextDoc
import tri.ai.text.chunks.TextDocMetadata
import tri.ai.text.chunks.process.LocalFileManager.extractMetadata
import tri.ai.text.chunks.process.PdfMetadataGuesser
import tri.promptfx.AiProgressView
import tri.promptfx.PromptFxController
import tri.promptfx.ui.docs.TextLibraryViewModel.Companion.mergeIn
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
    private val metadataModel: MetadataValidatorModel by inject()
    private val metadataGuessPageCount = SimpleIntegerProperty(2)

    /** Flag tracking whether changes have been made. */
    private val changesApplied = booleanProperty(false)

    init {
        val fileProps = doc.metadata.asGmvPropList("", isOriginal = true)
        metadataModel.props.setAll(fileProps)
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
                top = vbox {
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
                    }
                    toolbar {
                        visibleWhen(this@PdfViewerWithMetadataUi.progress.activeProperty)
                        managedWhen(this@PdfViewerWithMetadataUi.progress.activeProperty)
                        spacer()
                        progressbar(progress.indicator.progressProperty())
                        label(progress.label.textProperty())
                    }
                    toolbar {
                        visibleWhen(metadataModel.isChanged)
                        managedWhen(metadataModel.isChanged)
                        text("Updated Properties:")
                        button("Apply Changes", FontAwesomeIcon.SAVE.graphic) {
                            enableWhen(metadataModel.isChanged)
                            tooltip("Update metadata with any marked changes.")
                            action { applyPendingChanges() }
                        }
                        button("Reset", FontAwesomeIcon.UNDO.graphic) {
                            enableWhen(metadataModel.isChanged)
                            tooltip("Reset any changes that are pending.")
                            action { revertPendingChanges() }
                        }
                        button("Discard Unsaved", FontAwesomeIcon.TRASH.graphic) {
                            tooltip("Discard any properties from the list below that have not been saved.")
                            enableWhen(metadataModel.isAnyUnsavedNew)
                            action { discardUnsavedNew() }
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
                    val confirm = confirmClose()
                    if (confirm == ButtonType.OK) {
                        close()
                        onClose(metadataModel)
                    }
                }
            }
            button("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE) {
                action {
                    close()
                }
            }
        }
    }

    //region USER ACTIONS

    /** Confirm closing the window if there are unsaved changes. */
    private fun confirmClose(): ButtonType {
        var result: ButtonType = ButtonType.CANCEL
        if (!metadataModel.isChanged.value) {
            if (changesApplied.value) {
                alert(
                    Alert.AlertType.CONFIRMATION, "Confirm Changes", "Save changes made to metadata?",
                    ButtonType.OK, ButtonType.CANCEL, title = "Confirm Changes", owner = currentWindow
                ) {
                    result = it
                }
            } else {
                result = ButtonType.OK
            }
        } else {
            val changeDescription = metadataModel.pendingChangeDescription()
            alert(
                Alert.AlertType.CONFIRMATION, "Apply the following changes to the metadata?", changeDescription,
                ButtonType.OK, ButtonType.CANCEL, title = "Confirm Changes", owner = currentWindow
            ) {
                if (it == ButtonType.OK)
                    metadataModel.applyPendingChanges()
                result = it
            }
        }
        return result
    }

    /** Extract metadata for the given document if possible. */
    private fun executeMetadataExtraction() {
        if (pdfFile.exists()) {
            val md = pdfFile.extractMetadata()
            if (md.isNotEmpty()) {
                val metadata = TextDocMetadata("")
                metadata.mergeIn(md)
                metadataModel.merge(metadata.asGmvPropList("File Metadata", isOriginal = false), filterUnique = true)
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
            metadataModel.merge(it.asGmvPropList(isOriginal = false), filterUnique = false)
        }
    }

    /** Apply any pending changes to the metadata. */
    private fun applyPendingChanges() {
        val changeDescription = metadataModel.pendingChangeDescription()
        confirm(title = "Confirm Changes", header = "Apply the following changes to the metadata?", content = changeDescription, owner = currentWindow) {
            metadataModel.applyPendingChanges()
            changesApplied.set(true)
        }
    }

    /** Revert any pending changes to the metadata. */
    private fun revertPendingChanges() {
        val changeDescription = metadataModel.pendingChangeDescription()
        confirm(title = "Confirm Revert", header = "Reset the following changes to the metadata?", content = changeDescription, owner = currentWindow) {
            metadataModel.revertPendingChanges()
        }
    }

    /** Remove any unsaved properties from the list. */
    private fun discardUnsavedNew() {
        val changeDescription = metadataModel.unsavedNewValuesDescription()
        confirm(title = "Confirm Remove", header = "Remove the following properties with unsaved changes?", content = changeDescription, owner = currentWindow) {
            metadataModel.discardUnsavedNew()
        }
    }

    //endregion

}
