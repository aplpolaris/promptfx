/*-
 * #%L
 * promptkt-0.1.0-SNAPSHOT
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
import kotlinx.coroutines.runBlocking
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import tri.ai.openai.OpenAiEmbeddingService
import tri.util.ANSI_GREEN
import tri.util.ANSI_RESET
import tri.util.info
import tri.util.warning
import java.io.File
import java.io.IOException
import kotlin.system.exitProcess

/** An embedding index that loads the documents from the local file system. */
class LocalEmbeddingIndex(val root: File, val embeddingService: EmbeddingService) : EmbeddingIndex {

    var maxChunkSize: Int = 1000

    private val embeddingIndex = mutableMapOf<String, EmbeddingDocument>()

    override fun documentUrl(doc: EmbeddingDocument) = doc.originalUrl(root)

    override fun readSnippet(doc: EmbeddingDocument, section: EmbeddingSection) = doc.readText(root, section)

    //region INDEXERS

    private fun rootFiles(ext: String) = root.listFiles { _, name -> name.endsWith(ext) }?.toList() ?: emptyList()

    /** Command to reindex just new documents. */
    private suspend fun reindexNew(): Map<String, EmbeddingDocument> {
        preprocessDocumentFormats()
        val newDocs = rootFiles(".txt").filter {
            !embeddingIndex.containsKey(it.absolutePath) && !embeddingIndex.containsKey(it.name)
        }
        val newEmbeddings = mutableMapOf<String, EmbeddingDocument>()
        if (newDocs.isNotEmpty()) {
            newDocs.forEach {
                try {
                    newEmbeddings[it.name] = calculateEmbeddingSections(it)
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
        val docs = mutableMapOf<String, EmbeddingDocument>()
        rootFiles(".txt").forEach {
            docs[it.absolutePath] = calculateEmbeddingSections(it)
        }
        embeddingIndex.putAll(docs)
        saveIndex(root, embeddingIndex)
    }

    /** Chunks the document and calculates the embedding for each chunk. */
    private suspend fun calculateEmbeddingSections(file: File) =
        embeddingService.chunkedEmbedding(file.absolutePath, file.readText(), maxChunkSize)

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
    suspend fun getEmbeddingIndex(): MutableMap<String, EmbeddingDocument> {
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

    private fun restoreIndex(root: File): Map<String, EmbeddingDocument> {
        var index = File(root, indexFile()).let {
            if (it.exists())
                MAPPER.readValue<Map<String, EmbeddingDocument>>(it)
            else
                emptyMap()
        }
        if (index.keys.any { it.localPath(root) != it }) {
            info<LocalEmbeddingIndex>("Updating embeddings index to use local file references...")
            index = index.map { (_, doc) ->
                val localized = doc.copyWithLocalPath(root)
                localized.path to localized
            }.toMap()
            saveIndex(root, index)
            info<LocalEmbeddingIndex>("Completed.")
        }
        return index
    }

    private fun EmbeddingDocument.copyWithLocalPath(root: File) =
        EmbeddingDocument(path.localPath(root)).also {
            it.sections.addAll(sections)
        }

    /** Update a string representing a full path to just reperesent a local path relative to the given file. */
    private fun String.localPath(root: File): String {
        val pathSplit = if ("\\" in this) split("\\") else split("/")
        return if (pathSplit.size > 1 && pathSplit[pathSplit.size - 2] == root.name)
            pathSplit.last()
        else
            this
    }

    private fun saveIndex(root: File, index: Map<String, EmbeddingDocument>) =
        MAPPER.writerWithDefaultPrettyPrinter()
            .writeValue(File(root, indexFile()), index)

    companion object {
        private val MAPPER = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())

        @JvmStatic
        fun main(args: Array<String>) {
            val sampleArgs = arrayOf("D:\\data\\chatgpt\\doc-insight-test", "--reindex-new", "--max-chunk-size=1000")
            println("""
                $ANSI_GREEN
                Arguments expected:
                  <root folder> <options>
                Options:
                  --reindex-all
                  --reindex-new (default)
                  --max-chunk-size=<size> (default 1000)
                $ANSI_RESET
            """.trimIndent())

            if (args.isEmpty())
                exitProcess(0)

            val path = args[0]
            val reindexAll = args.contains("--reindex-all")
            val reindexNew = args.contains("--reindex-new") || !reindexAll
            val maxChunkSize = args.find { it.startsWith("--max-chunk-size") }?.substringAfter("=", "")?.toIntOrNull() ?: 1000

            val root = File(path)
            val embeddingService = OpenAiEmbeddingService()
            val index = LocalEmbeddingIndex(root, embeddingService)
            index.maxChunkSize = maxChunkSize
            runBlocking {
                if (reindexNew) {
                    println("Reindexing new documents in $root...")
                    index.getEmbeddingIndex() // this triggers the reindex
                } else if (reindexAll) {
                    println("Reindexing all documents in $root...")
                    index.reindexAll()
                } else {
                    TODO("Impossible to get here.")
                }
                println("Reindexing complete.")
            }
            index.saveIndex(root, index.embeddingIndex)
            exitProcess(0)
        }
    }
}
