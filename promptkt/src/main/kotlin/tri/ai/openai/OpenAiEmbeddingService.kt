package tri.ai.openai

import tri.ai.embedding.EmbeddingService
import tri.ai.embedding.TextChunk
import tri.ai.embedding.TextChunker.chunkBySections

/** An embedding service that uses the OpenAI API. */
class OpenAiEmbeddingService(val client: OpenAiClient = OpenAiClient.INSTANCE) : EmbeddingService {

    private val embeddingCache = mutableMapOf<String, List<Double>>()

    override suspend fun calculateEmbedding(text: List<String>): List<List<Double>> {
        val res = client.quickEmbedding(inputs = text).value
            ?: throw IllegalStateException("No embedding returned.")
        res.forEachIndexed { index, list -> embeddingCache[text[index]] = list }
        return res
    }

    override fun chunkTextBySections(text: String, maxChunkSize: Int) =
        TextChunk(text).chunkBySections(maxChunkSize, combineShortSections = true)

}
