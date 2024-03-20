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

import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import tri.ai.text.chunks.*
import java.io.File
import java.time.Instant
import java.time.LocalDateTime

/**
 * Manages set of [TextDoc]s coming from a local file structure.
 * Designed to save/restore metadata and chunk information to the file system.
 */
class LocalTextDocIndex(
    private val rootFolder: File,
    _indexFile: File? = null
) {

    /** Index of documents by id. */
    val docIndex = mutableMapOf<String, Pair<File, TextDoc>>()

    private val indexFile = _indexFile ?: File(rootFolder, "docs.json")

    //region PROCESSING

    /** Scrapes text from documents in index. */
    fun processDocuments(reindexAll: Boolean) {
        preprocessDocumentFormats(rootFolder, reindexAll)
        if (reindexAll) {
            docIndex.clear()
        }
        val docs = mutableMapOf<String, Pair<File, TextDoc>>()
        rootFolder.rootFiles(".txt").forEach {
            if (reindexAll || it.absolutePath !in docs) {
                val book = TextDoc(it.name).apply {
                    metadata.title = it.nameWithoutExtension
                    metadata.date = LocalDateTime.ofInstant(Instant.ofEpochMilli(it.lastModified()), java.time.ZoneId.systemDefault()).toLocalDate()
                    metadata.path = it.absolutePath
                    metadata.relativePath = it.name
                }
                docs[book.metadata.id] = it to book
            }
        }
        if (reindexAll)
            docIndex.putAll(docs)
        else
            docIndex.putAll(docs.filter { it.key !in docIndex })
    }

    /** Breaks up documents into chunks. */
    fun processChunks(chunker: TextChunker, reindexAll: Boolean) {
        docIndex.values.forEach {
            chunker.process(it.first, it.second, reindexAll)
        }
    }

    /** Breaks up a single document into chunks. */
    fun TextChunker.process(file: File, doc: TextDoc, reindexAll: Boolean) {
        if (doc.chunks.isEmpty() || reindexAll) {
            doc.chunks.clear()

            doc.all = TextChunkRaw(file.readText())
            doc.chunks.addAll(chunk(doc.all!!))
        }
    }

    //endregion

    //region SAVING INDEX

    /** Loads index from file. */
    fun loadIndex() {
        if (indexFile.exists()) {
            docIndex.clear()
            val index = TextLibrary.loadFrom(indexFile)
            index.docs.forEach { doc ->
                docIndex[doc.metadata.id] = fileFor(doc.metadata) to doc
            }
        }
    }

    /** Saves index to file. */
    fun saveIndex() {
        val index = TextLibrary("").apply {
            docs.addAll(docIndex.values.map { it.second })
        }
        TextLibrary.saveTo(index, indexFile)
    }

    //endregion

    companion object {
        /** Get the file associated with the metadata. */
        fun fileFor(metadata: TextDocMetadata): File {
            // TODO - allow absolute and relative path attempts to find file
            val file = File(metadata.path ?: throw IllegalStateException("File path not found in metadata"))
            require(file.exists()) { "File not found: ${metadata.path} ... TODO try relative path automatically" }
            return file
        }


        //region ALTERNATE FORMAT PROCESSING

        /**
         * Convert docs in other formats to text files if they don't already exist.
         */
        internal fun preprocessDocumentFormats(folder: File, reprocessAll: Boolean = false) {
            require(folder.isDirectory)

            fun File.process(ext: String, op: (File) -> String) = rootFiles(ext).preprocess(ext, reprocessAll, op)

            folder.process(".pdf", ::pdfText)
            folder.process(".docx", ::docxText)
            folder.process(".doc", ::docText)
        }

        private fun List<File>.preprocess(ext: String, reprocessAll: Boolean, op: (File) -> String) {
            forEach {
                val txtFile = File(it.absolutePath.replace(ext, ".txt"))
                if (reprocessAll || !txtFile.exists()) {
                    txtFile.writeText(op(it))
                }
            }
        }

        //endregion

        //region CONVERT FILES TO TEXT

        /** Check if file is a plaintext file, or can be converted to text, based on extension. */
        // TODO - think about how to calibrate supported extensions
        fun File.isFileWithText() = extension in setOf("pdf", "docx", "doc") ||
                extension in setOf("txt", "csv")
//                extension in setOf("txt", "md", "html", "htm", "xml", "json", "csv", "tsv")

        /** Get text from a file by extension. */
        // TODO - think about how to calibrate supported extensions
        fun File.fileToText() = when (extension) {
            "pdf" -> pdfText(this)
            "docx" -> docxText(this)
            "doc" -> docText(this)
            else -> readText()
        }

        /** Extract text from PDF. */
        private fun pdfText(file: File) = tri.ai.embedding.pdfText(file)

        /** Extract text from DOCX. */
        private fun docxText(file: File) = XWPFWordExtractor(XWPFDocument(file.inputStream())).text

        /** Extract text from DOC. */
        private fun docText(file: File) = WordExtractor(file.inputStream()).text

        //endregion

        /** Get all files in the root folder with the given extension. */
        internal fun File.rootFiles(ext: String) = listFiles { _, name -> name.endsWith(ext) }?.toList() ?: emptyList()
    }

}
