package tri.ai.embedding

/** An interface for chunking text and calculating embeddings. */
interface EmbeddingService {

    /**
     * Divide text at reasonable positions. Each chunk should have a length of at most maxChunkSize.
     * The returned map contains the start and end indices of each chunk.
     */
    fun chunkTextBySections(text: String, maxChunkSize: Int): List<TextChunk>

    /** Calculate embedding for multiple texts. */
    suspend fun calculateEmbedding(text: List<String>): List<List<Double>>

    /** Calculate embedding for a single text. */
    suspend fun calculateEmbedding(text: String): List<Double> =
        calculateEmbedding(listOf(text)).first()

    /** Calculate embedding for a single text. */
    suspend fun calculateEmbedding(vararg text: String): List<List<Double>> =
        calculateEmbedding(listOf(*text))

    /** Chunks a text into sections and calculates the embedding for each section. */
    suspend fun chunkedEmbedding(path: String, text: String, maxChunkSize: Int): EmbeddingDocument {
        println("Calculating embedding for $path...")
        val res = EmbeddingDocument(path)
        val chunks = chunkTextBySections(text, maxChunkSize)
        val chunkEmbeddings = calculateEmbedding(chunks.map { it.text })
        val embeddingTriples = chunks.zip(chunkEmbeddings).map {
            Triple(it.first.range, it.first.text, it.second)
        }
        embeddingTriples.forEach { (range, _, embedding) ->
            res.sections.add(EmbeddingSection(embedding, range.first, range.last))
        }
        return res
    }

}