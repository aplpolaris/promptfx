package tri.ai.embedding

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File

/** An embedding index that loads the documents from the local file system. */
class LocalEmbeddingIndex(val root: File, val embeddingService: EmbeddingService) : EmbeddingIndex {

    var maxChunkSize: Int = 1000

    private val embeddingIndex = mutableMapOf<String, EmbeddingDocument>()

    //region INDEXERS

    private fun rootFiles(ext: String) = root.listFiles { _, name -> name.endsWith(ext) }?.toList() ?: emptyList()

    /** Command to reindex just new documents. */
    private suspend fun reindexNew(): Map<String, EmbeddingDocument> {
        preprocessDocumentFormats()
        val newDocs = rootFiles(".txt").filter { !embeddingIndex.containsKey(it.absolutePath) }
        val newEmbeddings = mutableMapOf<String, EmbeddingDocument>()
        if (newDocs.isNotEmpty()) {
            newDocs.forEach {
                newEmbeddings[it.absolutePath] = calculateEmbeddingSections(it)
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

    private val textStripper = PDFTextStripper()

    /** Extract text from PDF. */
    private fun pdfText(file: File): String {
        PDDocument.load(file).use {
            return textStripper.getText(it)
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
                EmbeddingMatch(doc, section, cosineSimilarity(queryEmbedding, section.embedding))
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

    companion object {
        const val INDEX_FILE = "embeddings.json"

        private val MAPPER = ObjectMapper()
            .registerModule(KotlinModule())

        fun restoreIndex(root: File): Map<String, EmbeddingDocument> =
            File(root, INDEX_FILE).let {
                if (it.exists())
                    MAPPER.readValue(File(root, INDEX_FILE))
                else
                    emptyMap()
            }

        fun saveIndex(root: File, index: Map<String, EmbeddingDocument>) =
            MAPPER.writerWithDefaultPrettyPrinter()
                .writeValue(File(root, INDEX_FILE), index)
    }
}