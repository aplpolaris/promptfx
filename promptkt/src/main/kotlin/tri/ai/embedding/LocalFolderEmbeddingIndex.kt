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

import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import tri.ai.text.chunks.TextChunk
import tri.ai.text.chunks.TextDoc
import tri.ai.text.chunks.TextLibrary
import tri.ai.text.chunks.process.LocalFileManager
import tri.ai.text.chunks.process.LocalFileManager.listFilesWithTextContent
import tri.ai.text.chunks.process.TextDocEmbeddings.getEmbeddingInfo
import tri.util.loggerFor
import java.io.File
import java.net.URI

/** An embedding index that loads the documents from the local file system. */
class LocalFolderEmbeddingIndex(val rootDir: File, val embeddingService: EmbeddingService) : EmbeddingIndex {

    var maxChunkSize: Int = 1000

    val indexFile by lazy { File(rootDir, "embeddings2.json") }

    val library: TextLibrary by lazy {
        try {
            TextLibrary.loadFrom(rootDir)
        } catch (x: Exception) {
            loggerFor<LocalFolderEmbeddingIndex>().warning("Failed to load embedding index from $rootDir: ${x.message}")
            TextLibrary()
        }
    }

    /** List of files that can be chunked and embedded. */
    fun chunkableFiles() = rootDir.listFilesWithTextContent()

    /**
     * Reindex just new documents, calculating and storing embedding vectors for them.
     * Saves over existing library of chunked document embeddings if there are any changes.
     */
    suspend fun reindexNew() {
        val docsWithEmbeddings = library.docs.filter {
            it.chunks.any { it.getEmbeddingInfo(embeddingService.modelId) != null }
        }.mapNotNull { it.metadata.path }
        val newDocs = chunkableFiles().map { it.toURI() }.filter { it !in docsWithEmbeddings }
        newDocs.forEach {
            library.docs += calculateDocChunksAndEmbeddings(it, it.readText())
        }
        if (newDocs.isNotEmpty())
            TextLibrary.saveTo(library, indexFile)
    }

    /**
     * Reindex all documents, calculating and storing embedding vectors for them.
     * Replaces the existing library of chunked document embeddings.
     */
    suspend fun reindexAll() {
        val updatedDocs = chunkableFiles().map { it.toURI() }.map {
            calculateDocChunksAndEmbeddings(it, it.readText())
        }
        library.docs.clear()
        library.docs.addAll(updatedDocs)
        TextLibrary.saveTo(library, indexFile)
    }

    /** Read text from a URI, assuming for this class it must be a file that exists. */
    private fun URI.readText() = LocalFileManager.readText(this)

    //region INDEXERS

    /** Chunks the document and calculates the embedding for each chunk. */
    private suspend fun calculateDocChunksAndEmbeddings(uri: URI, text: String) =
        embeddingService.chunkedEmbedding(uri, text, maxChunkSize)

    //endregion

    //region ALTERNATE FORMAT PROCESSING

    /** Extract text from DOCX. */
    private fun docxText(file: File) = XWPFWordExtractor(XWPFDocument(file.inputStream())).text

    /** Extract text from DOC. */
    private fun docText(file: File) = WordExtractor(file.inputStream()).text

    //endregion

    private fun TextLibrary.docChunks(): List<Pair<TextDoc, TextChunk>> =
        docs.flatMap { doc ->
            doc.chunks.map { chunk -> doc to chunk }
        }

    override suspend fun findMostSimilar(query: String, n: Int): List<EmbeddingMatch> {
        val semanticTextQuery = embeddingService.calculateEmbedding(query).let {
            SemanticTextQuery(query, it, embeddingService.modelId)
        }
        val matches = library.docChunks().map { (doc, chunk) ->
            val chunkEmbedding = chunk.getEmbeddingInfo(embeddingService.modelId)!!
            EmbeddingMatch(semanticTextQuery, doc, chunk,
                chunkEmbedding,
                cosineSimilarity(semanticTextQuery.embedding, chunkEmbedding)
            )
        }
        return matches.sortedByDescending { it.score }.take(n)
    }

    /** Gets embedding index, processing new files and overwriting saved library if needed. */
    suspend fun calculateAndGetDocs(): List<TextDoc> {
        reindexNew()
        return library.docs.toList()
    }

}
