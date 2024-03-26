package tri.promptfx.tools

import javafx.beans.binding.Bindings
import javafx.beans.property.*
import tornadofx.*
import tri.ai.text.chunks.TextChunk
import tri.ai.text.chunks.TextChunkRaw
import tri.ai.text.chunks.TextDoc
import tri.ai.text.chunks.TextLibrary
import tri.ai.text.chunks.process.*
import tri.ai.text.chunks.process.LocalTextDocIndex.Companion.fileToText
import tri.ai.text.chunks.process.LocalTextDocIndex.Companion.isFileWithText
import tri.promptfx.ui.TextChunkViewModel
import tri.promptfx.ui.asTextChunkViewModel
import java.io.File
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.regex.PatternSyntaxException

/** Model for the [TextChunkerWizard]. */
class TextChunkerWizardModel: ViewModel() {

    // inputs
    var sourceToggleSelection: ObjectProperty<String> = SimpleObjectProperty(TcwSourceMode.FILE.uiName)
    val sourceMode = sourceToggleSelection.objectBinding { TcwSourceMode.valueOfUiName(it) }

    var isFileMode = sourceToggleSelection.isEqualTo(TcwSourceMode.FILE.uiName)!!
    val file = SimpleObjectProperty<File>()
    val fileName = file.stringBinding { it?.name ?: "None" }

    val isFolderMode = sourceToggleSelection.isEqualTo(TcwSourceMode.FOLDER.uiName)!!
    val folder = SimpleObjectProperty<File>()
    val folderName = folder.stringBinding { it?.name ?: "None" }
    val folderIncludeSubfolders = SimpleObjectProperty(false)
    val folderExtractText = SimpleObjectProperty(true)

    val isUserInputMode = sourceToggleSelection.isEqualTo(TcwSourceMode.USER_INPUT.uiName)!!
    val userText = SimpleStringProperty()

    val isWebScrapeMode = sourceToggleSelection.isEqualTo(TcwSourceMode.WEB_SCRAPING.uiName)!!
    val webScrapeModel = WebScrapeViewModel()

    val isRssMode = sourceToggleSelection.isEqualTo(TcwSourceMode.RSS_FEED.uiName)!!
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
    var chunkMethodSelection: ObjectProperty<String> = SimpleObjectProperty(TextChunkerWizardMethod.CHUNK_AUTO)
    val isCleanUpWhiteSpace = SimpleBooleanProperty(true)
    val isChunkAutomatic = chunkMethodSelection.isEqualTo(TextChunkerWizardMethod.CHUNK_AUTO)!!
    private val maxChunkSize = SimpleObjectProperty(1000)
    internal val isChunkDelimiter = chunkMethodSelection.isEqualTo(TextChunkerWizardMethod.CHUNK_BY_DELIMITER)
    internal val chunkDelimiter = SimpleStringProperty("\n")
    internal val isChunkRegex = chunkMethodSelection.isEqualTo(TextChunkerWizardMethod.CHUNK_BY_REGEX)
    internal val chunkRegex = SimpleStringProperty("\\n{2,}")
    internal val isChunkField = chunkMethodSelection.isEqualTo(TextChunkerWizardMethod.CHUNK_BY_FIELD)

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
    private fun inputTextSample() =
        sourceMode.value!!.inputTextSample(this)

    /** Get all text based on current settings, with multiple strings returned if multiple files. */
    private fun inputDocs(progressUpdate: (String) -> Unit) =
        sourceMode.value!!.allInputText(this, progressUpdate)

    /** Update the preview of chunks based on the current settings. */
    internal fun updatePreview() {
        val MAX_PREVIEW_CHUNKS = 100
        val inputTextSample = inputTextSample()
        val chunker = chunker()
        val docChunk = TextChunkRaw(inputTextSample)
        val chunks = chunker.chunk(docChunk)
            .filter(chunkFilter(docChunk))
            .take(MAX_PREVIEW_CHUNKS)
        previewChunks.setAll(chunks.map { it.asTextChunkViewModel(docChunk, null) })
    }

    /** Chunker based on current settings. */
    private fun chunker(): TextChunker = when {
        isChunkAutomatic.get() ->
            StandardTextChunker(maxChunkSize.value)
        isChunkDelimiter.get() ->
            DelimiterTextChunker(
                isCleanUpWhiteSpace.value, listOf(chunkDelimiter.value
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .ifEmpty { "\n" })
            )
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
        val addedChunks = mutableSetOf<String>()
        return inputDocs(progressUpdate).mapNotNull { doc ->
            val docChunk = doc.all!!
            val docChunks = chunker.chunk(docChunk).filter(chunkFilter(docChunk))
            if (!chunkFilterRemoveDuplicates.value)
                doc.chunks.addAll(docChunks)
            else
                docChunks.forEach {
                    val chunkText = it.text(docChunk)
                    if (chunkText !in addedChunks) {
                        doc.chunks.add(it)
                        addedChunks.add(chunkText)
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

/** Modes for the chunker wizard. */
enum class TcwSourceMode(val uiName: String) {
    FILE("File") {
        override fun inputTextSample(model: TextChunkerWizardModel) =
            model.file.value?.fileToText(true) ?: ""
        override fun allInputText(model: TextChunkerWizardModel, progressUpdate: (String) -> Unit) =
            listOf(model.file.value!!.asTextDoc())
    },

    FOLDER("Folder") {
        override fun inputTextSample(model: TextChunkerWizardModel) =
            model.folder.value?.walkTopDown()
                ?.filter { it.isFileWithText() }
                ?.firstOrNull()
                ?.fileToText(true) ?: ""
        override fun allInputText(model: TextChunkerWizardModel, progressUpdate: (String) -> Unit): List<TextDoc> {
            val folder = model.folder.value!!
            folder.walkTopDown()
                .filter { it.isDirectory }
                .forEach {
                    progressUpdate("Preprocessing ${it.absolutePath}...")
                    LocalTextDocIndex.preprocessDocumentFormats(it)
                }
            return folder.walkTopDown()
                .filter { it.extension.lowercase() == "txt" }
                .map {
                    val original = LocalTextDocIndex.originalFileFor(it)
                    original.asTextDoc()
                }.toList()
        }

    },

    USER_INPUT("User Input") {
        override fun inputTextSample(model: TextChunkerWizardModel) =
            model.userText.value
        override fun allInputText(model: TextChunkerWizardModel, progressUpdate: (String) -> Unit): List<TextDoc> {
            val t0 = LocalDateTime.now()
            return listOf(TextDoc("User Input $t0", TextChunkRaw(model.userText.value)).apply {
                metadata.title = "User Input $t0"
                metadata.author = System.getProperty("user.name")
                metadata.date = t0.toLocalDate()
            })
        }
    },

    WEB_SCRAPING("Web Scraping") {
        override fun inputTextSample(model: TextChunkerWizardModel) =
            model.webScrapeModel.mainUrlText()
        override fun allInputText(model: TextChunkerWizardModel, progressUpdate: (String) -> Unit): List<TextDoc> =
            model.webScrapeModel.scrapeWebsite(progressUpdate).map {
                TextDoc(it.key.toString(), TextChunkRaw(it.value)).apply {
                    metadata.title = it.key.toString()
                    metadata.date = LocalDate.now()
                }
            }
    },

    RSS_FEED("RSS Feed") {
        override fun inputTextSample(model: TextChunkerWizardModel) = "" // TODO
        override fun allInputText(model: TextChunkerWizardModel, progressUpdate: (String) -> Unit): List<TextDoc> = listOf() // TODO
    };

    internal fun File.asTextDoc(): TextDoc {
        val uri = toURI()
        val raw = TextChunkRaw(fileToText(useExistingTxtFile = true))
        return TextDoc(uri.toString(), raw).apply {
            metadata.title = nameWithoutExtension // TODO - can we be smarter about this?
            // TODO metadata.author =
            metadata.date = lastModified().let { LocalDate.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) }
            metadata.path = uri.path
            metadata.relativePath = relativeTo(File(parent)).path
        }
    }

    /** Get sample of input text based on current settings. */
    abstract fun inputTextSample(model: TextChunkerWizardModel): String

    /** Get all input text based on current settings, organized as [TextDoc]s. */
    abstract fun allInputText(model: TextChunkerWizardModel, progressUpdate: (String) -> Unit): List<TextDoc>

    companion object {
        fun valueOfUiName(name: String?) = values().firstOrNull { it.uiName == name }
    }

}