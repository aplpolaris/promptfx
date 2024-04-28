/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.text.chunks.process

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileFilter
import java.lang.runtime.ObjectMethods
import java.net.URI
import java.time.LocalDate
import java.time.ZoneId

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
        val file2 = File(alternateFolder, file.name)
        return when {
            file2.exists() -> file2
            file.exists() -> file
            else -> null
        }
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
    /** Map a file to an associated metadata file. */
    fun File.metadataFile(): File {
        val name = nameWithoutExtension
        return File(parentFile, "$name.meta.json")
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
            DOC -> WordDocUtils.readDoc(this)
            DOCX -> WordDocUtils.readDocx(this)
            else -> readText()
        }.also {
            if (useCache) {
                txtFile.writeText(it)
                val props = when (extension) {
                    PDF -> pdfMetadata(this)
                    DOC -> WordDocUtils.readDocMetadata(this)
                    DOCX -> WordDocUtils.readDocxMetadata(this)
                    else -> emptyMap()
                }.filterValues { it != null && (it !is String || it.isNotBlank()) }
                if (props.isNotEmpty())
                    ObjectMapper()
                        .registerModule(JavaTimeModule())
                        .writerWithDefaultPrettyPrinter()
                        .writeValue(metadataFile(), props)
            }
        }
    }

    /** Extract text from PDF. */
    private fun pdfText(file: File) = tri.ai.embedding.pdfText(file)
    /** Extract metadata from PDF. */
    private fun pdfMetadata(file: File) = tri.ai.embedding.pdfMetadata(file)

    //endregion
}

/** Utilities for working with Word documents. */
object WordDocUtils {

    /** Read text from a DOC file. */
    fun readDoc(file: File) = WordExtractor(file.inputStream()).use { it.text }
    /** Read metadata from a DOC file. */
    fun readDocMetadata(file: File) = WordExtractor(file.inputStream()).use {
        it.summaryInformation.let {
            mapOf(
                "doc.title" to it.title,
                "doc.author" to it.author,
                "doc.subject" to it.subject,
                "doc.keywords" to it.keywords,
                "doc.comments" to it.comments,
                "doc.template" to it.template,
                "doc.lastAuthor" to it.lastAuthor,
                "doc.revNumber" to it.revNumber,
                "doc.createTime" to it.createDateTime,
                "doc.editTime" to it.editTime
            )
        }
    }

    /** Read text from a DOCX file. */
    fun readDocx(file: File) = XWPFWordExtractor(XWPFDocument(file.inputStream())).use { it.text }
    /** Read metadata from a DOCX file. */
    fun readDocxMetadata(file: File) = XWPFDocument(file.inputStream()).use {
        it.properties.coreProperties.let {
            mapOf(
                "docx.title" to it.title,
                "docx.author" to it.creator,
                "docx.subject" to it.subject,
                "docx.category" to it.category,
                "docx.keywords" to it.keywords,
                "docx.description" to it.description,
                "docx.created" to it.created,
                "docx.modified" to it.modified,
                "docx.modifiedBy" to it.lastModifiedByUser,
                "docx.contentStatus" to it.contentStatus,
                "docx.contentType" to it.contentType,
                "docx.version" to it.version,
                "docx.revision" to it.revision,
            )
        }
    }

}
