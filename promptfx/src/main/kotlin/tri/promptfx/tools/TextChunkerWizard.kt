package tri.promptfx.tools

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.binding.Bindings
import javafx.beans.property.*
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.text.chunks.TextChunk
import tri.ai.text.chunks.TextChunkRaw
import tri.ai.text.chunks.TextDoc
import tri.ai.text.chunks.TextLibrary
import tri.ai.text.chunks.process.*
import tri.ai.text.chunks.process.LocalTextDocIndex.Companion.fileToText
import tri.ai.text.chunks.process.LocalTextDocIndex.Companion.isFileWithText
import tri.promptfx.tools.TextChunkerWizardMethod.Companion.CHUNK_AUTO
import tri.promptfx.tools.TextChunkerWizardMethod.Companion.CHUNK_BY_DELIMITER
import tri.promptfx.tools.TextChunkerWizardMethod.Companion.CHUNK_BY_FIELD
import tri.promptfx.tools.TextChunkerWizardMethod.Companion.CHUNK_BY_REGEX
import tri.promptfx.tools.TextChunkerWizardSelectData.Companion.FILE_OPTION
import tri.promptfx.tools.TextChunkerWizardSelectData.Companion.FOLDER_OPTION
import tri.promptfx.tools.TextChunkerWizardSelectData.Companion.RSS_FEED
import tri.promptfx.tools.TextChunkerWizardSelectData.Companion.USER_INPUT
import tri.promptfx.tools.TextChunkerWizardSelectData.Companion.WEB_SCRAPING
import tri.promptfx.ui.TextChunkListView
import tri.promptfx.ui.TextChunkViewModel
import tri.promptfx.ui.asTextChunkViewModel
import tri.util.ui.slider
import java.io.File
import java.net.URI
import java.net.URL
import java.util.regex.PatternSyntaxException

/** Model for the [TextChunkerWizard]. */
class TextChunkerWizardModel: ViewModel() {

    // inputs
    var sourceToggleSelection: ObjectProperty<String> = SimpleObjectProperty(FILE_OPTION)

    var isFileMode = sourceToggleSelection.isEqualTo(FILE_OPTION)!!
    val file = SimpleObjectProperty<File>()
    val fileName = file.stringBinding { it?.name ?: "None" }

    val isFolderMode = sourceToggleSelection.isEqualTo(FOLDER_OPTION)!!
    val folder = SimpleObjectProperty<File>()
    val folderName = folder.stringBinding { it?.name ?: "None" }
    val folderIncludeSubfolders = SimpleObjectProperty(false)
    val folderExtractText = SimpleObjectProperty(true)

    val isUserInputMode = sourceToggleSelection.isEqualTo(USER_INPUT)!!
    val userText = SimpleStringProperty()

    val isWebScrapeMode = sourceToggleSelection.isEqualTo(WEB_SCRAPING)!!
    val webScrapeModel = WebScrapeViewModel()

    val isRssMode = sourceToggleSelection.isEqualTo(RSS_FEED)!!
    val rssFeed = SimpleObjectProperty<URL>()

    // whether source has been properly selected
    val isSourceSelected = (isFileMode.and(file.isNotNull))
        .or(isFolderMode.and(folder.isNotNull))
        .or(isUserInputMode.and(userText.isNotEmpty))
        .or(isWebScrapeMode.and(
            webScrapeModel.webUrl.booleanBinding {
                !it.isNullOrBlank() && (it.startsWith("http://") || it.startsWith("https://"))
                        && it.substringAfter("//").isNotBlank()
            }
        ))
        .or(isRssMode.and(rssFeed.isNotNull))!!

    // chunking options
    var chunkMethodSelection: ObjectProperty<String> = SimpleObjectProperty(CHUNK_AUTO)
    val isCleanUpWhiteSpace = SimpleBooleanProperty(true)
    val isChunkAutomatic = chunkMethodSelection.isEqualTo(CHUNK_AUTO)!!
    private val maxChunkSize = SimpleObjectProperty(1000)
    internal val isChunkDelimiter = chunkMethodSelection.isEqualTo(CHUNK_BY_DELIMITER)
    internal val chunkDelimiter = SimpleStringProperty("\n")
    internal val isChunkRegex = chunkMethodSelection.isEqualTo(CHUNK_BY_REGEX)
    internal val chunkRegex = SimpleStringProperty("\\n{2,}")
    internal val isChunkField = chunkMethodSelection.isEqualTo(CHUNK_BY_FIELD)

    // chunk filter
    val chunkFilterMinSize = SimpleIntegerProperty(50)
    val chunkFilterRemoveDuplicates = SimpleBooleanProperty(true)

    // preview of chunks
    val previewChunks = observableListOf<TextChunkViewModel>()

    // whether chunking method has been properly selected
    internal val isChunkMethodValid = Bindings.isNotEmpty(previewChunks)

    init {
        maxChunkSize.onChange { updatePreview() }
        isCleanUpWhiteSpace.onChange { updatePreview() }
        chunkMethodSelection.onChange { updatePreview() }
        chunkDelimiter.onChange { updatePreview() }
        chunkRegex.onChange { updatePreview() }
        chunkFilterMinSize.onChange { updatePreview() }
    }

    /** Get sample of input text based on current settings. */
    private fun inputTextSample(): String {
        return when {
            isFileMode.get() -> file.value?.fileToText() ?: ""
            isFolderMode.get() -> folder.value?.walkTopDown()
                        ?.filter { it.isFileWithText() }
                        ?.firstOrNull()
                        ?.fileToText() ?: ""
            isUserInputMode.get() -> userText.value
            isWebScrapeMode.get() -> webScrapeModel.mainUrlText()
            isRssMode.get() -> TODO()
            else -> ""
        }
    }

    /** Get all text based on current settings, with multiple strings returned if multiple files. */
    private fun allInputText(progressUpdate: (String) -> Unit): Map<URI?, String> {
        return when {
            isFileMode.get() -> mapOf(file.value.toURI() to (file.value?.fileToText() ?: ""))
            isFolderMode.get() -> folder.value!!.walkTopDown()
                        .filter { it.isFileWithText() }
                        .associate { it.toURI() to it.fileToText() }
            isUserInputMode.get() -> mapOf(null to userText.value)
            isWebScrapeMode.get() -> webScrapeModel.scrapeWebsite(progressUpdate)
            isRssMode.get() -> TODO()
            else -> throw IllegalStateException("No source selected")
        }
    }

    /** Update the preview of chunks based on the current settings. */
    internal fun updatePreview() {
        val MAX_PREVIEW_CHUNKS = 100
        val inputTextSample = inputTextSample()
        val chunker = chunker()
        val docChunk = TextChunkRaw(inputTextSample)
        val chunks = chunker.chunk(docChunk)
            .filter(chunkFilter(docChunk))
            .take(MAX_PREVIEW_CHUNKS)
        previewChunks.setAll(chunks.map { it.asTextChunkViewModel(docChunk) })
    }

    /** Chunker based on current settings. */
    private fun chunker(): TextChunker = when {
        isChunkAutomatic.get() ->
            StandardTextChunker(maxChunkSize.value)
        isChunkDelimiter.get() ->
            DelimiterTextChunker(isCleanUpWhiteSpace.value, listOf(chunkDelimiter.value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .ifEmpty { "\n" }))
        isChunkRegex.get() -> try {
            RegexTextChunker(isCleanUpWhiteSpace.value, chunkRegex.value.toRegex())
        } catch (x: PatternSyntaxException) {
            // TODO - show error to user
            NoOpTextChunker
        }
        isChunkField.get() -> NoOpTextChunker // TODO
        else -> NoOpTextChunker
    }

    /** Get chunk filter. */
    fun chunkFilter(doc: TextChunk?): (TextChunk) -> Boolean = {
        it.text(doc).length >= chunkFilterMinSize.value
    }

    /** Get final chunks. */
    fun finalDocs(progressUpdate: (String) -> Unit): List<TextDoc> {
        val chunker = chunker()
        val inputText = allInputText(progressUpdate)
        val addedChunks = mutableSetOf<String>()
        return inputText.mapNotNull { (uri, text) ->
            val docChunk = TextChunkRaw(text)
            val docChunks = chunker.chunk(docChunk).filter(chunkFilter(docChunk))
            val doc = TextDoc(uri.toString(), docChunk).apply {
                if (!chunkFilterRemoveDuplicates.value)
                    chunks.addAll(docChunks)
                else
                    docChunks.forEach {
                        val chunkText = it.text(docChunk)
                        if (chunkText !in addedChunks) {
                            chunks.add(it)
                            addedChunks.add(chunkText)
                        }
                    }
            }
            if (doc.chunks.isNotEmpty()) doc else null
        }
    }

    fun finalLibrary(progressUpdate: (String) -> Unit): TextLibrary? {
        val sourceInfo = when {
            isFileMode.get() -> file.value?.toURI()?.toString()
            isFolderMode.get() -> folder.value?.toURI()?.toString()
            isUserInputMode.get() -> "User Input"
            isWebScrapeMode.get() -> webScrapeModel.webUrl.value
            isRssMode.get() -> rssFeed.value.toString()
            else -> "Unknown"
        }
        val finalDocs = finalDocs(progressUpdate)
        return if (finalDocs.isEmpty()) null
        else TextLibrary().apply {
            metadata.id = "Text Content from $sourceInfo"
            docs.addAll(finalDocs)
        }
    }

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
                    radiobutton(FILE_OPTION) {
                        graphic = FontAwesomeIconView(FontAwesomeIcon.FILE_TEXT)
                    }
                    radiobutton(FOLDER_OPTION) {
                        graphic = FontAwesomeIconView(FontAwesomeIcon.FOLDER_OPEN)
                    }
                    radiobutton(USER_INPUT) {
                        graphic = FontAwesomeIconView(FontAwesomeIcon.KEYBOARD_ALT)
                    }
                    radiobutton(WEB_SCRAPING) {
                        graphic = FontAwesomeIconView(FontAwesomeIcon.GLOBE)
                    }
                    radiobutton(RSS_FEED) {
                        isDisable = true
                        graphic = FontAwesomeIconView(FontAwesomeIcon.RSS)
                    }
                    model.sourceToggleSelection.bindBidirectional(selectedValueProperty())
                    model.sourceToggleSelection.set(FILE_OPTION)
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
                        model.file.value = chooseFile("Select File", filters = arrayOf(), mode = FileChooserMode.Single, owner = currentWindow).firstOrNull()
                    }
                }
                label("File:")
                label(model.fileName)
            }
            hbox(5, alignment = Pos.CENTER_LEFT) {
                visibleWhen(model.isFolderMode)
                managedWhen(model.isFolderMode)
                vbox(5) {
                    hbox {
                        button("Select...") {
                            action {
                                model.folder.value = chooseDirectory("Select Directory", owner = currentWindow)
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

    companion object {
        internal const val FILE_OPTION = "File"
        internal const val FOLDER_OPTION = "Directory"
        internal const val USER_INPUT = "User Input"
        internal const val WEB_SCRAPING = "Web Scraping"
        internal const val RSS_FEED = "RSS Feed"
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