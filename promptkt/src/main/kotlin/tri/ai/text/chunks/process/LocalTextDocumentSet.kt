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
import tri.ai.embedding.pdfText
import tri.ai.text.chunks.*
import java.io.File
import java.time.Instant
import java.time.LocalDateTime

/**
 * Document set managed within local file structure.
 * Designed to save/restore metadata and chunk information to the file system.
 */
class LocalTextDocumentSet(
    private val rootFolder: File,
    _indexFile: File? = null
) {

    /** Tracks documents by id. */
    val documents = mutableMapOf<String, Pair<File, TextBook>>()

    private val indexFile = _indexFile ?: File(rootFolder, "docs.json")

    //region PROCESSING

    /** Scrapes text from documents in index. */
    fun processDocuments(reindexAll: Boolean) {
        preprocessDocumentFormats(reindexAll)
        if (reindexAll) {
            documents.clear()
        }
        val docs = mutableMapOf<String, Pair<File, TextBook>>()
        rootFiles(".txt").forEach {
            if (reindexAll || it.absolutePath !in docs) {
                val book = TextBook(it.name).apply {
                    metadata.title = it.nameWithoutExtension
                    metadata.date = LocalDateTime.ofInstant(Instant.ofEpochMilli(it.lastModified()), java.time.ZoneId.systemDefault()).toLocalDate()
                    metadata.path = it.absolutePath
                    metadata.relativePath = it.name
                }
                docs[book.metadata.id] = it to book
            }
        }
        if (reindexAll)
            documents.putAll(docs)
        else
            documents.putAll(docs.filter { it.key !in documents })
    }

    /** Breaks up documents into chunks. */
    fun processChunks(chunker: TextChunker, reindexAll: Boolean) {
        documents.values.forEach {
            chunker.process(it.first, it.second, reindexAll)
        }
    }

    /** Breaks up a single document into chunks. */
    fun TextChunker.process(file: File, doc: TextBook, reindexAll: Boolean) {
        if (doc.chunks.isEmpty() || reindexAll) {
            doc.chunks.clear()

            val all = TextChunkRaw(file.readText())
            doc.chunks.addAll(chunk(all))
        }
    }

    //endregion

    //region SAVING INDEX

    /** Loads index from file. */
    fun loadIndex() {
        if (indexFile.exists()) {
            documents.clear()
            val index = TextLibrary.loadFrom(indexFile)
            index.books.forEach { doc ->
                // TODO - allow absolute and relative path attempts to find file
                val file = File(doc.metadata.path ?: throw IllegalStateException("File path not found in metadata"))
                require(file.exists()) { "File not found: ${doc.metadata.path} ... TODO try relative path automatically" }
                documents[doc.metadata.id] = file to doc
            }
        }
    }

    /** Saves index to file. */
    fun saveIndex() {
        val index = TextLibrary("").apply {
            books.addAll(documents.values.map { it.second })
        }
        TextLibrary.saveTo(index, indexFile)
    }

    //endregion

    //region FROM LocalEmbeddingIndex.kt

    private fun rootFiles(ext: String) = rootFolder.listFiles { _, name -> name.endsWith(ext) }?.toList() ?: emptyList()

    //region ALTERNATE FORMAT PROCESSING

    /**
     * Convert docs in other formats to text files if they don't already exist.
     */
    private fun preprocessDocumentFormats(reprocessAll: Boolean = false) {
        preprocess(".pdf", reprocessAll) { pdfText(it) }
        preprocess(".docx", reprocessAll) { docxText(it) }
        preprocess(".doc", reprocessAll) { docText(it) }
    }

    private fun preprocess(ext: String, reprocessAll: Boolean, op: (File) -> String) {
        rootFiles(ext).forEach {
            val txtFile = File(it.absolutePath.replace(ext, ".txt"))
            if (reprocessAll || !txtFile.exists()) {
                txtFile.writeText(op(it))
            }
        }
    }

    /** Extract text from DOCX. */
    private fun docxText(file: File) = XWPFWordExtractor(XWPFDocument(file.inputStream())).text

    /** Extract text from DOC. */
    private fun docText(file: File) = WordExtractor(file.inputStream()).text

    //endregion

    //endregion

}
