package tri.ai.text.chunks

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import tri.ai.embedding.pdfText
import tri.util.ANSI_GREEN
import tri.util.ANSI_RESET
import java.io.File
import kotlin.system.exitProcess

/** A collection of text documents. */
class TextDocumentSet {
    val documents = mutableListOf<TextDocument>()
    val chunks = mutableListOf<TextChunk>()
}

/** Serializable version of the document. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class SerializableDocument(
    val metadata: TextMetadata,
    val attributes: DocumentAttributes,
    val sections: List<TextSection>
) {
    constructor(doc: TextDocument, sections: List<TextSection>) : this(doc.metadata, doc.attributes, sections)
}

/** Document set managed within local file structure. Designed to save/restore metadata and chunk information to the file system. */
class LocalTextDocumentSet(
    @get:JsonIgnore
    val root: File
) {

    @get:JsonIgnore
    val docs = TextDocumentSet()

    val documents: List<SerializableDocument>
        get() = docs.chunks
            .filterIsInstance<TextSection>()
            .groupBy { it.doc }
            .entries
            .map { SerializableDocument(it.key.metadata, it.key.attributes, it.value) }

    fun reloadDocuments() {
        preprocessDocumentFormats()
        val docs = mutableMapOf<String, TextDocument>()
        rootFiles(".txt").forEach {
            docs[it.absolutePath] = TextDocumentImpl(it.absolutePath, it.readText())
        }
        this.docs.documents.addAll(docs.values)
    }

    fun reloadChunks(chunker: TextChunker) {
        docs.chunks.clear()
        docs.chunks.addAll(docs.documents.flatMap { chunker.chunk(it) })
    }

    fun saveIndex(target: File) {
        MAPPER.writerWithDefaultPrettyPrinter()
            .writeValue(target, this)
    }

    //region FROM LocalEmbeddingIndex.kt

    private fun rootFiles(ext: String) = root.listFiles { _, name -> name.endsWith(ext) }?.toList() ?: emptyList()

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

    //endregion

    companion object {
        private val MAPPER = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
    }
}

object LocalDocumentManager {
    /** Runnable for working with document sets. */
    @JvmStatic
    fun main(args: Array<String>) {
        val args = arrayOf("D:\\data\\chatgpt\\doc-insight-test", "--reindex-all", "--max-chunk-size=5000")
        println(
            """
            $ANSI_GREEN
            Arguments expected:
              <root folder> <options>
            Options:
              --reindex-all
              --reindex-new (default)
              --max-chunk-size=<size> (default 1000)
            $ANSI_RESET
        """.trimIndent()
        )

        if (args.isEmpty())
            exitProcess(0)

        val path = args[0]
        val reindexAll = args.contains("--reindex-all")
        val reindexNew = args.contains("--reindex-new") || !reindexAll
        val maxChunkSize =
            args.find { it.startsWith("--max-chunk-size") }?.substringAfter("=", "")?.toIntOrNull() ?: 1000

        println("Refreshing file text...")
        val docs = LocalTextDocumentSet(File(path))
        docs.reloadDocuments()

        println("Chunking documents...")
        val chunker = StandardTextChunker(maxChunkSize)
        docs.reloadChunks(chunker)
        docs.docs.chunks.forEach {
            when (it) {
                is TextSection -> println("  ${it.first} ${it.last} ${it.text}")
                else -> println("  $it")
            }
        }

        println("Saving document set info...")
        docs.saveIndex(File("docs.json"))

        println("Processing complete.")
        exitProcess(0)
    }
}