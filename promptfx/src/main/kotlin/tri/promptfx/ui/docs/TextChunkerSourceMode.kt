package tri.promptfx.ui.docs

import tri.ai.text.chunks.TextChunkRaw
import tri.ai.text.chunks.TextDoc
import tri.ai.text.chunks.process.LocalFileManager
import tri.ai.text.chunks.process.LocalFileManager.extractTextContent
import tri.ai.text.chunks.process.LocalFileManager.fileToText
import tri.ai.text.chunks.process.LocalFileManager.originalFile
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Modes for the chunker wizard. */
enum class TextChunkerSourceMode(val uiName: String) {
    FILE("File") {
        override fun inputTextSample(model: TextChunkerWizardModel) =
            model.file.value?.fileToText(true) ?: ""
        override fun allInputText(model: TextChunkerWizardModel, progressUpdate: (String) -> Unit) =
            listOf(model.file.value!!.asTextDoc(model.isHeaderRow.value))
    },

    FOLDER("Folder") {
        override fun inputTextSample(model: TextChunkerWizardModel) =
            model.folder.value?.walkTopDown()
                ?.filter(LocalFileManager.fileWithTextContentFilter::accept)
                ?.firstOrNull()
                ?.fileToText(true) ?: ""

        override fun allInputText(model: TextChunkerWizardModel, progressUpdate: (String) -> Unit): List<TextDoc> {
            val folder = model.folder.value!!
            folder.walkTopDown()
                .filter { it.isDirectory }
                .forEach {
                    progressUpdate("Extracting text from files in ${it.absolutePath}...")
                    it.extractTextContent(reprocessAll = true)
                }
            return folder.walkTopDown()
                .filter { it.extension.lowercase() == LocalFileManager.TXT }
                .map {
                    it.originalFile()!!.asTextDoc(model.isHeaderRow.value)
                }.toList()
        }
    },

    USER_INPUT("User Input") {
        override fun inputTextSample(model: TextChunkerWizardModel) =
            model.userText.value
        override fun allInputText(model: TextChunkerWizardModel, progressUpdate: (String) -> Unit): List<TextDoc> {
            return listOf(textDocWithHeader(
                "User Input ${LocalDateTime.now()}",
                model.userText.value,
                model.isHeaderRow.value
            ))
        }
    },

    WEB_SCRAPING("Web Scraping") {
        override fun inputTextSample(model: TextChunkerWizardModel) =
            model.webScrapeModel.mainUrlText()
        override fun allInputText(model: TextChunkerWizardModel, progressUpdate: (String) -> Unit): List<TextDoc> =
            model.webScrapeModel.scrapeWebsite(progressUpdate).map {
                textDocWithHeader(it.key.toString(), it.value, model.isHeaderRow.value)
            }
    },

    RSS_FEED("RSS Feed") {
        override fun inputTextSample(model: TextChunkerWizardModel) = "" // TODO
        override fun allInputText(model: TextChunkerWizardModel, progressUpdate: (String) -> Unit): List<TextDoc> = listOf() // TODO
    };

    /** Get sample of input text based on current settings. Should read data only, not apply processing options (e.g. remove header row). */
    abstract fun inputTextSample(model: TextChunkerWizardModel): String

    /** Get all input text based on current settings, organized as [TextDoc]s. Should apply processing options, e.g. remove header row. */
    abstract fun allInputText(model: TextChunkerWizardModel, progressUpdate: (String) -> Unit): List<TextDoc>

    companion object {
        fun valueOfUiName(name: String?) = values().firstOrNull { it.uiName == name }

        /** Create a [TextDoc] from a file with full raw text chunk, optionally the first line as the header. */
        internal fun File.asTextDoc(isFirstLineHeader: Boolean): TextDoc {
            val uri = toURI()
            val text = fileToText(useCache = true)
            val useText = if (isFirstLineHeader) text.substringAfter("\n").trim() else text
            return TextDoc(uri.toString(), TextChunkRaw(useText)).apply {
                metadata.title = nameWithoutExtension // TODO - can we be smarter about this?
                // TODO metadata.author =
                metadata.dateTime = lastModified().let {
                    LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(it),
                        ZoneId.systemDefault()
                    )
                }
                metadata.path = uri
                metadata.relativePath = relativeTo(File(parent)).path
                if (isFirstLineHeader)
                    dataHeader = text.substringBefore("\n").trim()
            }
        }

        /** Create a [TextDoc] from a text string with full raw text chunk, optionally the first line as the header. */
        internal fun textDocWithHeader(title: String, text: String, isFirstLineHeader: Boolean): TextDoc {
            val useText = if (isFirstLineHeader) text.substringAfter("\n").trim() else text
            return TextDoc(title, TextChunkRaw(useText)).apply {
                metadata.title = title
                metadata.author = System.getProperty("user.name")
                metadata.dateTime = LocalDateTime.now()
                if (isFirstLineHeader)
                    dataHeader = text.substringBefore("\n").trim()
            }
        }
    }

}