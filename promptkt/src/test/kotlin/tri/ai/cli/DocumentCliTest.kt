package tri.ai.cli

import com.github.ajalt.clikt.core.subcommands
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class DocumentCliTest {

    private fun main(args: Array<String>) =
        DocumentCli()
            .subcommands(DocumentChat(), DocumentChunker(), DocumentEmbeddings(), DocumentQa())
            .main(args)

    @Test
    fun testChunk() {
        val path = Path("C:\\data\\chatgpt\\test3")
        main(arrayOf(
            "--root=$path",
            "chunk",
            "--reindex-all"
        ))
    }

    @Test
    fun testEmbeddings() {
        val path = Path("C:\\data\\chatgpt\\test3")
        main(arrayOf(
            "--root=$path",
            "embeddings",
            "--reindex-all"
        ))
    }
}