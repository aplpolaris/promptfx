package tri.ai.text.chunks.process

import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileFilter
import java.net.URI

/**
 * Handles local files, including extraction of text from various formats, catching of text in associated ".txt" files,
 * managing of changes to paths, and retrieval of original files.
 */
object LocalFileManager {

    const val TXT = "txt"
    const val PDF = "pdf"
    private const val DOCX = "docx"
    private const val DOC = "doc"

    /** Extensions supported by the embedding index, either raw text or with available scrapers. */
    private val SUPPORTED_EXTENSIONS = listOf(PDF, DOCX, DOC, TXT)

    /**
     * Attempt to fix a file path, when the file may have been moved to another directory.
     * Returns the original file location, if it exists, or the file inside the alternate folder,
     * if there is a matching file with the same name.
     */
    fun fixPath(file: File, alternateFolder: File): File? {
        require(alternateFolder.isDirectory)
        if (file.exists())
            return file
        val file2 = File(alternateFolder, file.name)
        if (file2.exists())
            return file2
        return null
    }

    //region FILE MANAGEMENT

    /** Return file filter for files that are convertible to text, but not a text cache file themselves. */
    val fileWithTextContentFilter = FileFilter { file -> file.hasTextContent() && !file.isLikelyTextCache() }

    /** List all files with .txt extension in a given folder. */
    fun File.textFiles() = listFiles { f -> f.extension.lowercase() == TXT }?.toList() ?: emptyList()

    /** List files that are convertible to text in a given folder. */
    fun File.listFilesWithTextContent(): List<File> {
        require(isDirectory)
        return listFiles(fileWithTextContentFilter)?.toList() ?: emptyList()
    }

    /** Return true if the file is convertible to text. */
    private fun File.hasTextContent() = extension.lowercase() in SUPPORTED_EXTENSIONS

    /** Return true if the file has a .txt extension and there is an associated "original" file that likely matches. */
    private fun File.isLikelyTextCache() = extension.lowercase() == TXT && originalFile()?.extension?.lowercase() != TXT

    /** Find a non .txt file that might be associated with a .txt file. */
    fun File.originalFile(): File? {
        val name = nameWithoutExtension
        return SUPPORTED_EXTENSIONS.map { File(parentFile, "$name.$it") }
            .firstOrNull { it.exists() }
    }

    /** Map a file to an associated .txt file. */
    fun File.textCacheFile(): File {
        val name = nameWithoutExtension
        return File(parentFile, "$name.txt")
    }

    //endregion

    //region SCRAPING

    /**
     * Reads text content from a given URI, or the text file matching its contents.
     * Throws an exception if URI is not a file.
     */
    fun readText(uri: URI) =
        File(uri).fileToText(true)

    /** Scrape all documents with text content in a folder. */
    fun File.extractTextContent(reprocessAll: Boolean = false) {
        require(isDirectory)
        listFiles {
                f -> f.hasTextContent() && f.extension.lowercase() != TXT
                && (reprocessAll || !f.textCacheFile().exists())
        }?.forEach {
            it.fileToText(true)
        }
    }

    /**
     * Get text from a file by extension.
     * @param useCache if true, reads/writes to a .txt file in the same directory, creating it if it doesn't already exist
     */
    fun File.fileToText(useCache: Boolean): String {
        val txtFile = textCacheFile()
        if (useCache && txtFile.exists()) {
            return txtFile.readText()
        }
        return when (extension) {
            PDF -> pdfText(this)
            DOCX -> docxText(this)
            DOC -> docText(this)
            else -> readText()
        }.also {
            if (useCache) {
                txtFile.writeText(it)
            }
        }
    }

    /** Extract text from PDF. */
    private fun pdfText(file: File) = tri.ai.embedding.pdfText(file)

    /** Extract text from DOCX. */
    private fun docxText(file: File) = XWPFWordExtractor(XWPFDocument(file.inputStream())).text

    /** Extract text from DOC. */
    private fun docText(file: File) = WordExtractor(file.inputStream()).text

    //endregion
}