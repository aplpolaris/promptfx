package tri.promptfx.tools

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.text.chunks.TextChunk
import tri.promptfx.PromptFxConfig.Companion.DIR_KEY_TXT
import tri.promptfx.PromptFxConfig.Companion.FF_ALL
import tri.promptfx.promptFxDirectoryChooser
import tri.promptfx.promptFxFileChooser
import tri.promptfx.ui.TextChunkListView
import tri.util.ui.slider

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

/** Wizard step for selecting the source of data to chunk. */
class TextChunkerWizardSelectData: View("Select Source") {

    val model: TextChunkerWizardModel by inject()

    override val complete = model.isSourceSelected

    override val root = vbox(5) {
        // source data selection
        vbox(5) {
            label("Select source for data:")
            toolbar {
                togglegroup {
                    radiobutton(TcwSourceMode.FILE.uiName) {
                        graphic = FontAwesomeIconView(FontAwesomeIcon.FILE_TEXT)
                    }
                    radiobutton(TcwSourceMode.FOLDER.uiName) {
                        graphic = FontAwesomeIconView(FontAwesomeIcon.FOLDER_OPEN)
                    }
                    radiobutton(TcwSourceMode.USER_INPUT.uiName) {
                        graphic = FontAwesomeIconView(FontAwesomeIcon.KEYBOARD_ALT)
                    }
                    radiobutton(TcwSourceMode.WEB_SCRAPING.uiName) {
                        graphic = FontAwesomeIconView(FontAwesomeIcon.GLOBE)
                    }
                    radiobutton(TcwSourceMode.RSS_FEED.uiName) {
                        isDisable = true
                        graphic = FontAwesomeIconView(FontAwesomeIcon.RSS)
                    }
                    model.sourceToggleSelection.bindBidirectional(selectedValueProperty())
                    model.sourceToggleSelection.set(TcwSourceMode.FILE.uiName)
                }
            }
        }

        // detailed options for the source
        vbox(10) {
            hbox(5, alignment = Pos.CENTER_LEFT) {
                visibleWhen(model.isFileMode)
                managedWhen(model.isFileMode)
                button("Select...") {
                    action {
                        promptFxFileChooser(
                            dirKey = DIR_KEY_TXT,
                            title = "Select File",
                            filters = arrayOf(FF_ALL),
                            mode = FileChooserMode.Single
                        ) {
                            model.file.value = it.firstOrNull()
                        }
                    }
                }
                label("File:")
                label(model.fileName)
            }
            vbox(5) {
                visibleWhen(model.isFolderMode)
                managedWhen(model.isFolderMode)
                hbox(5, Pos.CENTER_LEFT) {
                    button("Select...") {
                        action {
                            promptFxDirectoryChooser(
                                dirKey = DIR_KEY_TXT,
                                title = "Select Directory"
                            ) {
                                model.folder.value = it
                            }
                        }
                    }
                    label("Folder:")
                    label(model.folderName)
                }
                checkbox("Include subfolders", model.folderIncludeSubfolders) {
                    // TODO - scripting over subfolders might be too aggressive
                    isDisable = true
//                        enableWhen(model.folder.isNotNull)
                }
                checkbox("Extract text from PDF, DOC files", model.folderExtractText) {
                    enableWhen(model.folder.isNotNull)
                }
            }
            hbox(5) {
                visibleWhen(model.isUserInputMode)
                managedWhen(model.isUserInputMode)
                textarea(model.userText) {
                    prefColumnCount = 80
                    prefRowCount = 10
                    promptText = "Enter or paste text to chunk here..."
                }
                vgrow = Priority.ALWAYS
            }
            hbox(5) {
                visibleWhen(model.isWebScrapeMode)
                managedWhen(model.isWebScrapeMode)
                add(find<WebScrapeFragment>("model" to model.webScrapeModel) {
                    isShowLocalFolder.set(false)
                })
            }
            hbox(5) {
                visibleWhen(model.isRssMode)
                managedWhen(model.isRssMode)
                text("TODO")
            }
        }
    }
}

/** Wizard step for selecting the method for chunking the data. */
class TextChunkerWizardMethod: View("Configure Chunking") {

    val model: TextChunkerWizardModel by inject()

    override val complete = model.isChunkMethodValid

    private lateinit var chunkPreview: TextChunkListView

    override fun onDock() {
        super.onDock()
        model.updatePreview()
    }

    override val root = vbox(5) {
        // method for determining chunks from data
        vbox(5) {
            label("Select method for chunking:")
            toolbar {
                togglegroup {
                    radiobutton(CHUNK_AUTO) {
                        selectToggle(this)
                        graphic = FontAwesomeIconView(FontAwesomeIcon.MAGIC)
                        tooltip = tooltip("Automatically split text into chunks based on common patterns.")
                    }
                    radiobutton(CHUNK_BY_DELIMITER) {
                        graphic = FontAwesomeIconView(FontAwesomeIcon.CUT)
                        tooltip = tooltip("Split text into chunks based on a character string.")
                    }
                    radiobutton(CHUNK_BY_REGEX) {
                        graphic = FontAwesomeIconView(FontAwesomeIcon.CODE)
                        tooltip = tooltip("Split text into chunks based on a regular expression.")
                    }
                    radiobutton(CHUNK_BY_FIELD) {
                        graphic = FontAwesomeIconView(FontAwesomeIcon.FILE_CODE_ALT)
                        tooltip = tooltip("Select a field in a CSV or JSON file to use as the source of text chunks.")
                    }
                    model.chunkMethodSelection.bindBidirectional(selectedValueProperty())
                    model.chunkMethodSelection.set(CHUNK_AUTO)
                }
            }
        }

        // detailed options for chunking
        form {
            fieldset("Automatic Chunking Options") {
                visibleWhen(model.isChunkAutomatic)
                managedWhen(model.isChunkAutomatic)
                field("TBD")
            }
            fieldset("Delimited Chunking Options") {
                visibleWhen(model.isChunkDelimiter)
                managedWhen(model.isChunkDelimiter)
                tooltip("Character(s) separating chunks of input, e.g. \\n or \\r for line breaks, \\t for tabs, or \\n\\n for paragraphs." +
                        "\nDefaults to \\n if left blank.")
                field("Preprocess") {
                    checkbox("Clean up white space", model.isCleanUpWhiteSpace) {
                        tooltip("Standardize white space before splitting text.")
                    }
                }
                field("Delimiter") {
                    textfield(model.chunkDelimiter) {
                        hgrow = Priority.ALWAYS
                        promptText = "Enter delimiter separating chunks (\\n or \\r for line breaks, \\t for tabs)"
                    }
                }
            }
            fieldset("Regex Chunking Options") {
                visibleWhen(model.isChunkRegex)
                managedWhen(model.isChunkRegex)
                field("Preprocess") {
                    checkbox("Clean up white space", model.isCleanUpWhiteSpace) {
                        tooltip("Standardize white space before splitting text.")
                    }
                }
                field("Regex") {
                    textfield(model.chunkRegex) {
                        hgrow = Priority.ALWAYS
                        promptText = "Enter a regular expression..."
                    }
                }
            }
            fieldset("Field Chunking Options") {
                visibleWhen(model.isChunkField)
                managedWhen(model.isChunkField)
                field("TBD")
            }

            // filter options
            fieldset("Filtering Options") {
                field("Minimum chunk size") {
                    slider(1..1000, model.chunkFilterMinSize)
                    label(model.chunkFilterMinSize)
                }
                field("Duplicates") {
                    checkbox("Remove duplicates", model.chunkFilterRemoveDuplicates)
                }
            }
        }

        // preview of text chunks
        separator()
        vbox(5) {
            prefWidth = 800.0
            label("Preview of chunks:")
            chunkPreview = TextChunkListView(model.previewChunks, null, hostServices)
            add(chunkPreview)
        }
    }

    companion object {
        internal const val CHUNK_AUTO = "Automatic"
        internal const val CHUNK_BY_DELIMITER = "Delimiter"
        internal const val CHUNK_BY_REGEX = "Regex"
        internal const val CHUNK_BY_FIELD = "Field"
    }
}