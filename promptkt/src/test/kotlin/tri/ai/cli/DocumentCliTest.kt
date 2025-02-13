package tri.ai.cli

import com.github.ajalt.clikt.core.subcommands
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.readText

class DocumentCliTest {

    private fun main(args: Array<String>) =
        DocumentCli()
            .subcommands(DocumentChat(), DocumentChunker(), DocumentEmbeddings(), DocumentQa())
            .main(args)

    private val path = Path("C:\\data\\chatgpt\\test3")

    @Test
    fun testChunk() {
        main(arrayOf(
            "--root=$path",
            "chunk",
            "--reindex-all"
        ))
        println(path.resolve("docs.json").readText())
    }

    @Test
    fun testEmbeddings() {
        main(arrayOf(
            "--root=$path",
            "embeddings",
            "--reindex-all"
        ))
    }

    @Test
    fun testQa() {
        main(arrayOf(
            "--root=$path",
            "qa",
            "What is Llama?"
        ))
    }
}