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

import com.aallam.openai.api.exception.OpenAIException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import tri.ai.openai.OpenAiEmbeddingService
import tri.ai.text.chunks.process.LocalFileManager
import tri.ai.text.chunks.process.LocalFileManager.TXT
import tri.util.info
import tri.util.warning
import java.io.File
import java.io.IOException
import java.net.URI

/** An embedding index that loads the documents from the local file system. */
class LocalEmbeddingIndex(val root: File, val embeddingService: EmbeddingService) : EmbeddingIndex {

    var maxChunkSize: Int = 1000

    val embeddingIndex = mutableMapOf<URI, EmbeddingDocument>()

    override fun readSnippet(doc: EmbeddingDocument, section: EmbeddingSection) = doc.readText(section)

    //region INDEXERS

    private fun rootFiles(ext: String) = root.listFiles { _, name -> name.endsWith(ext) }?.toList() ?: emptyList()

    /** Command to reindex just new documents. */
    private suspend fun reindexNew(): Map<URI, EmbeddingDocument> {
        preprocessDocumentFormats()
        val newDocs = rootFiles(".txt").filter {
            !embeddingIndex.containsKey(it.toURI())
        }
        val newEmbeddings = mutableMapOf<URI, EmbeddingDocument>()
        if (newDocs.isNotEmpty()) {
            newDocs.forEach {
                try {
                    newEmbeddings[it.toURI()] = calculateEmbeddingSections(it)
                } catch (x: IOException) {
                    warning<LocalEmbeddingIndex>("Failed to calculate embeddings for $it: $x")
                } catch (x: OpenAIException) {
                    warning<LocalEmbeddingIndex>("Failed to calculate embeddings for $it: $x")
                }
            }
        }
        return newEmbeddings
    }

    /** Command to reindex all documents from scratch */
    suspend fun reindexAll() {
        embeddingIndex.clear()
        preprocessDocumentFormats()
        val docs = mutableMapOf<URI, EmbeddingDocument>()
        rootFiles(".$TXT").forEach {
            docs[it.toURI()] = calculateEmbeddingSections(it)
        }
        embeddingIndex.putAll(docs)
        saveIndex(root, embeddingIndex)
    }

    /** Chunks the document and calculates the embedding for each chunk. */
    private suspend fun calculateEmbeddingSections(file: File) =
        embeddingService.chunkedEmbedding(file.toURI(), file.readText(), maxChunkSize)

    //endregion

    //region ALTERNATE FORMAT PROCESSING

    /** Convert docs in other formats to text files if they don't already exist. */
    private fun preprocessDocumentFormats() {
        preprocess(".pdf") { pdfText(it) }
        preprocess(".docx") { docxText(it) }
        preprocess(".doc") { docText(it) }
    }

    private fun preprocess(ext: String, op: (File) -> String) {
        rootFiles(ext).forEach {
            val txtFile = File(it.absolutePath.replace(ext, ".txt"))
            if (!txtFile.exists()) {
                txtFile.writeText(op(it))
            }
        }
    }

    /** Extract text from DOCX. */
    private fun docxText(file: File) = XWPFWordExtractor(XWPFDocument(file.inputStream())).text

    /** Extract text from DOC. */
    private fun docText(file: File) = WordExtractor(file.inputStream()).text

    //endregion

    override suspend fun findMostSimilar(query: String, n: Int): List<EmbeddingMatch> {
        val queryEmbedding = embeddingService.calculateEmbedding(query)
        val matches = getEmbeddingIndex().values.flatMap { doc ->
            doc.sections.map { section ->
                EmbeddingMatch(doc, section, queryEmbedding, cosineSimilarity(queryEmbedding, section.embedding))
            }
        }
        return matches.sortedByDescending { it.score }.take(n)
    }

    /** Gets embedding index, processing new files if needed. */
    suspend fun getEmbeddingIndex(): MutableMap<URI, EmbeddingDocument> {
        if (embeddingIndex.isEmpty())
            embeddingIndex.putAll(restoreIndex(root))
        reindexNew().let {
            if (it.isNotEmpty()) {
                embeddingIndex.putAll(it)
                saveIndex(root, embeddingIndex)
            }
        }
        return embeddingIndex
    }

    private fun indexFile() = when {
        embeddingService is OpenAiEmbeddingService -> "embeddings.json"
        else -> "embeddings-${embeddingService.modelId.replace("/", "_")}.json"
    }

    private fun restoreIndex(root: File): Map<URI, EmbeddingDocument> {
        val index = File(root, indexFile()).let {
            if (it.exists())
                MAPPER.readValue<Map<String, EmbeddingDocument>>(it)
            else
                emptyMap()
        }
        val updatedKeys = mutableMapOf<String, String>()
        index.keys.forEach {
            // old formats were saved as file paths, so we need to update them to URIs
            val fixedFileUri = LocalFileManager.fixPath(File(it), root)?.toURI()?.toString()
            if (fixedFileUri != it && fixedFileUri != null)
                updatedKeys[it] = fixedFileUri
        }
        val uriIndex = mutableMapOf<URI, EmbeddingDocument>()
        index.forEach {
            val uriString = updatedKeys[it.key] ?: it.key
            uriIndex[URI.create(uriString)] = it.value
        }
        if (updatedKeys.isNotEmpty()) {
            info<LocalEmbeddingIndex>("Updating embeddings index to use URIs...")
            saveIndex(root, uriIndex)
            info<LocalEmbeddingIndex>("Completed.")
        }
        return uriIndex
    }

    private fun EmbeddingDocument.copyWithFixedPath(rootDir: File) =
        EmbeddingDocument(LocalFileManager.fixPath(File(uri), rootDir)!!.toURI()).also {
            it.sections.addAll(sections)
        }

    fun saveIndex(root: File, index: Map<URI, EmbeddingDocument>) =
        MAPPER.writerWithDefaultPrettyPrinter()
            .writeValue(File(root, indexFile()), index)

    companion object {
        private val MAPPER = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
    }
}
