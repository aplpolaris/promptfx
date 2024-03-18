package tri.promptfx.tools

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.text.chunks.TextChunk
import tri.promptfx.ui.TextChunkListView
import tri.promptfx.ui.TextChunkViewModel
import java.io.File

/** Model for the [TextChunkerWizard]. */
class TextChunkerWizardModel: ViewModel() {

    // inputs
    var sourceToggleSelection: ObjectProperty<String> = SimpleObjectProperty("File/Directory")
    val file = SimpleObjectProperty<File>()
    val fileName = file.stringBinding { it?.name ?: "None" }
    val userText = SimpleStringProperty()

    // chunking options
    var chunkMethodSelection: ObjectProperty<String> = SimpleObjectProperty("Automatic")

    // preview of chunks
    val previewChunks = observableListOf<TextChunkViewModel>()

}

/** Wizard for generating a set of [TextChunk]s from user input. */
class TextChunkerWizard: Wizard("Text Chunker", "Generate a set of text chunks from user input or selected source.") {

    val model: TextChunkerWizardModel by inject()

    override val canGoNext = currentPageComplete
    override val canFinish = allPagesComplete

    init {
        add(TextChunkerWizardSelectData::class)
        add(TextChunkerWizardMethod::class)
    }
}

class TextChunkerWizardSelectData: View("Select Source") {

    val model: TextChunkerWizardModel by inject()

    override val complete = SimpleBooleanProperty(true)

    override val root = vbox(5) {
        // source data selection
        vbox(5) {
            label("Select source for data:")
            flowpane {
                togglegroup {
                    radiobutton("File/Directory") {
                        isSelected = true
                        graphic = FontAwesomeIconView(FontAwesomeIcon.FILE_TEXT_ALT)
                    }
                    radiobutton("User Input") {
                        graphic = FontAwesomeIconView(FontAwesomeIcon.KEYBOARD_ALT)
                    }
                    radiobutton("Web Scraping") {
                        graphic = FontAwesomeIconView(FontAwesomeIcon.INTERNET_EXPLORER)
                    }
                    model.sourceToggleSelection = selectedValueProperty()
                }
            }
        }

        // detailed options for the source
        vbox(5) {
            hbox {
                enableWhen(model.sourceToggleSelection.isEqualTo("File/Directory"))
                label("File/Folder: ")
                label(model.fileName)
            }
            hbox {
                enableWhen(model.sourceToggleSelection.isEqualTo("User Input"))
                textarea(model.userText) {
                    promptText = "Enter text here..."
                }
            }
            hbox {
                enableWhen(model.sourceToggleSelection.isEqualTo("Web Scraping"))
                label("TBD")
            }
        }
    }
}

class TextChunkerWizardMethod: View("Configure Chunking") {

    val model: TextChunkerWizardModel by inject()

    private lateinit var chunkPreview: TextChunkListView

    override val root = vbox(5) {
        // method for determining chunks from data
        vbox(5) {
            label("Select method for chunking:")
            flowpane {
                togglegroup {
                    radiobutton("Automatic") {
                        isSelected = true
                        graphic = FontAwesomeIconView(FontAwesomeIcon.ALIGN_JUSTIFY)
                        tooltip = tooltip("Automatically split text into chunks based on common patterns.")
                    }
                    radiobutton("Split by character/regex") {
                        graphic = FontAwesomeIconView(FontAwesomeIcon.CUT)
                        tooltip = tooltip("Split text into chunks based on a character or regular expression.")
                    }
                    radiobutton("Field in CSV/JSON") {
                        graphic = FontAwesomeIconView(FontAwesomeIcon.FONT)
                        tooltip = tooltip("Select a field in a CSV or JSON file to use as the source of text chunks.")
                    }
                    model.chunkMethodSelection = selectedValueProperty()
                }
            }
        }

        // detailed options for chunking
        vbox(5) {
            hbox {
                enableWhen(model.chunkMethodSelection.isEqualTo("Split by character/regex"))
                label("Character/Regex: ")
                textfield()
            }
            hbox {
                enableWhen(model.chunkMethodSelection.isEqualTo("Field in CSV/JSON"))
                label("TBD")
            }
        }

        // preview of text chunks
        vbox(5) {
            label("Preview of chunks:")
            chunkPreview = TextChunkListView(model.previewChunks, null, hostServices)
            add(chunkPreview)
        }
    }

}