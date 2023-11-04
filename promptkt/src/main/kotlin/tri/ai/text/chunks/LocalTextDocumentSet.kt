/*-
 * #%L
 * promptkt-0.1.12-SNAPSHOT
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.text.chunks

import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import tri.ai.embedding.pdfText
import java.io.File

/**
 * Document set managed within local file structure.
 * Designed to save/restore metadata and chunk information to the file system.
 */
class LocalTextDocumentSet(
    private val rootFolder: File,
    _indexFile: File? = null
) {

    val documents = mutableMapOf<String, TextDocument>()
    val chunks = mutableListOf<TextChunk>()

    private val indexFile = _indexFile ?: File(rootFolder, "docs.json")

    //region PROCESSING

    /** Scrapes text from documents in index. */
    fun processDocuments(reindexAll: Boolean) {
        preprocessDocumentFormats(reindexAll)
        if (reindexAll) {
            documents.clear()
            chunks.clear()
        }
        val docs = mutableMapOf<String, TextDocumentImpl>()
        rootFiles(".txt").forEach {
            if (reindexAll || it.absolutePath !in docs)
                docs[it.absolutePath] = TextDocumentImpl(it)
        }
        documents.putAll(docs)
    }

    /** Breaks up documents into chunks. */
    fun processChunks(chunker: TextChunker, reindexAll: Boolean): List<TextChunk> {
        val newChunks = if (reindexAll) {
            chunks.clear()
            documents.flatMap { chunker.chunk(it.value) }
        } else {
            val existing = chunks.filterIsInstance<TextSection>().map { it.doc.metadata.id }.toSet()
            documents.filterKeys { it !in existing }.flatMap { chunker.chunk(it.value) }
        }
        chunks.addAll(newChunks)
        return newChunks
    }

    //endregion

    //region SAVING INDEX

    /** Loads index from file. */
    fun loadIndex() {
        if (indexFile.exists()) {
            documents.clear()
            chunks.clear()
            val index = LocalDocumentIndex.loadFrom(indexFile)
            val docsLookup = index.documents.associateWith { TextDocumentImpl(File(it.metadata.id)) }
            documents.putAll(docsLookup.mapKeys { it.key.metadata.id })
            chunks.addAll(index.documents.flatMap { doc ->
                doc.sections.map { TextSection(docsLookup[doc]!!, it.first..it.last) }
            })
        }
    }

    /** Saves index to file. */
    fun saveIndex() {
        val index = LocalDocumentIndex().apply {
            documents = chunks
                .filterIsInstance<TextSection>()
                .groupBy { it.doc }
                .entries
                .map {
                    LocalDocumentInfo(it.key.metadata, it.key.attributes, it.value.map { TextSectionInfo(it) })
                }
        }
        LocalDocumentIndex.saveTo(index, indexFile)
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
