package tri.ai.embedding

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tri.ai.embedding.TextChunker.chunkBySections
import kotlin.io.path.toPath

@OptIn(ExperimentalCoroutinesApi::class)
class LocalEmbeddingIndexTest {

    @Test
    fun `test index`() = runTest {
        val docsPath = LocalEmbeddingIndexTest::class.java.getResource("resources")!!.toURI().toPath().toFile()
        val index = LocalEmbeddingIndex(docsPath, MockEmbeddingService())
        index.getEmbeddingIndex().forEach { (path, doc) ->
            println(path)
            doc.sections.take(10).forEach { section ->
                val text = doc.readText(section).replace("\\s+".toRegex(), " ")
                println("  ${section.start} ${section.end} ${text.take(200)}")
            }
        }
    }

}

class MockEmbeddingService: EmbeddingService {
    override fun chunkTextBySections(text: String, maxChunkSize: Int): List<TextChunk> {
        return TextChunk(text).chunkBySections(maxChunkSize, combineShortSections = true)
    }

    override suspend fun calculateEmbedding(text: List<String>): List<List<Double>> {
        return text.map { listOf(1.0) }
    }
}