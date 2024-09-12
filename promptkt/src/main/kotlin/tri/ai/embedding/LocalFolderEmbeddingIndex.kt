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
package tri.ai.embedding

import tri.ai.text.chunks.TextChunk
import tri.ai.text.chunks.TextDoc
import tri.ai.text.chunks.TextLibrary
import tri.ai.text.chunks.process.LocalFileManager
import tri.ai.text.chunks.process.LocalFileManager.listFilesWithTextContent
import tri.ai.text.chunks.process.TextDocEmbeddings.calculateMissingEmbeddings
import tri.ai.text.chunks.process.TextDocEmbeddings.chunkedEmbedding
import tri.ai.text.chunks.process.TextDocEmbeddings.getEmbeddingInfo
import tri.util.loggerFor
import java.io.File
import java.net.URI

/** An embedding index that loads the documents from the local file system. */
class LocalFolderEmbeddingIndex(val rootDir: File, val embeddingService: EmbeddingService) : EmbeddingIndex {

    var maxChunkSize: Int = 1000

    val indexFile by lazy { File(rootDir, EMBEDDINGS_FILE_NAME) }

    private val library: TextLibrary by lazy {
        try {
            TextLibrary.loadFrom(indexFile).apply {
                if (metadata.path.isNullOrEmpty()) {
                    metadata.path = rootDir.toURI().toString()
                }
            }
        } catch (x: Exception) {
            loggerFor<LocalFolderEmbeddingIndex>().warning("Failed to load embedding index from $rootDir: ${x.message}")
            TextLibrary().apply {
                metadata.id = rootDir.name
                metadata.path = rootDir.toURI().toString()
            }
        }
    }

    /** List of files that can be chunked and embedded. */
    fun chunkableFiles() = rootDir.listFilesWithTextContent()

    /**
     * Reindex just new documents, calculating and storing embedding vectors for them.
     * Saves over existing library of chunked document embeddings if there are any changes.
     */
    suspend fun reindexNew() {
        val allPaths = chunkableFiles().map { it.toURI() }
        val docsWithEmbeddings = library.docs.filter {
            it.chunks.any { it.getEmbeddingInfo(embeddingService.modelId) != null }
        }.toSet()

        // add to existing chunks that just need embedding calculations
        val docsNeedingEmbeddings = (library.docs - docsWithEmbeddings).toSet()
        docsNeedingEmbeddings.forEach { it.calculateMissingEmbeddings(embeddingService) }

        // add new documents from file system that were not in library
        val newDocs = allPaths - library.docs.mapNotNull { it.metadata.path }.toSet()
        newDocs.forEach {
            library.docs += calculateDocChunksAndEmbeddings(it, it.readText())
        }

        if (newDocs.isNotEmpty() || docsNeedingEmbeddings.isNotEmpty())
            saveIndex()
    }

    /**
     * Reindex all documents, calculating and storing embedding vectors for them.
     * This may remove existing embedding calculations from other models.
     * Replaces the existing library of chunked document embeddings.
     */
    suspend fun reindexAll() {
        val updatedDocs = chunkableFiles().map { it.toURI() }.map {
            calculateDocChunksAndEmbeddings(it, it.readText())
        }
        library.docs.clear()
        library.docs.addAll(updatedDocs)
        saveIndex()
    }

    /** Adds a document to the library if it is not already present. */
    fun addIfNotPresent(it: TextDoc): Boolean {
        if (library.docs.any { doc -> doc.metadata.path == it.metadata.path })
            return false
        library.docs.add(it)
        return true
    }

    /** Saves the index to file. */
    fun saveIndex() {
        TextLibrary.saveTo(library, indexFile)
    }

    /** Read text from a URI, assuming for this class it must be a file that exists. */
    private fun URI.readText() = LocalFileManager.readText(this)

    //region INDEXERS

    /** Chunks the document and calculates the embedding for each chunk. */
    private suspend fun calculateDocChunksAndEmbeddings(uri: URI, text: String) =
        embeddingService.chunkedEmbedding(uri, text, maxChunkSize)

    //endregion

    private fun TextLibrary.docChunks(): List<Pair<TextDoc, TextChunk>> =
        docs.flatMap { doc ->
            doc.chunks.map { chunk -> doc to chunk }
        }

    override suspend fun findMostSimilar(query: String, n: Int): List<EmbeddingMatch> {
        val modelId = embeddingService.modelId
        val semanticTextQuery = SemanticTextQuery(query, embeddingService.calculateEmbedding(query), modelId)
        reindexNew()
        val matches = library.docChunks().map { (doc, chunk) ->
            val chunkEmbedding = chunk.getEmbeddingInfo(modelId)!!
            EmbeddingMatch(semanticTextQuery, doc, chunk,
                modelId, chunkEmbedding,
                cosineSimilarity(semanticTextQuery.embedding, chunkEmbedding).toFloat()
            )
        }
        return matches.sortedByDescending { it.queryScore }.take(n)
    }

    /** Gets embedding index, processing new files and overwriting saved library if needed. */
    suspend fun calculateAndGetDocs(): List<TextDoc> {
        reindexNew()
        return library.docs.toList()
    }

    companion object {
        const val EMBEDDINGS_FILE_NAME = "embeddings2.json"
        const val EMBEDDINGS_FILE_NAME_LEGACY = "embeddings.json"
    }

}
