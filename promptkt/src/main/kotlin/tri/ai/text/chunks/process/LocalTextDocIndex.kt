/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
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

import tri.ai.text.chunks.*
import tri.ai.text.chunks.process.LocalFileManager.extractTextContent
import tri.ai.text.chunks.process.LocalFileManager.textCacheFile
import tri.ai.text.chunks.process.LocalFileManager.textFiles
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
        rootFolder.extractTextContent(reindexAll)
        if (reindexAll) {
            docIndex.clear()
        }
        val docs = mutableMapOf<String, Pair<File, TextDoc>>()
        rootFolder.textFiles().forEach {
            if (reindexAll || it.absolutePath !in docs) {
                val doc = it.createTextDoc()
                docs[doc.metadata.id] = it to doc
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
                docIndex[doc.metadata.id] = File(doc.metadata.path!!).textCacheFile() to doc
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

        /** Create a [TextDoc] with metadata from a file's attributes. */
        fun File.createTextDoc() = TextDoc(name).apply {
            metadata.title = nameWithoutExtension
            metadata.dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(lastModified()),
                java.time.ZoneId.systemDefault()
            )
            metadata.path = toURI()
            metadata.relativePath = name
        }
    }
}

