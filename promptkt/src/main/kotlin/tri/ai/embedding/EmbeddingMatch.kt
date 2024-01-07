package tri.ai.embedding

/** A scored match for a query. */
class EmbeddingMatch(
    val document: EmbeddingDocument,
    val section: EmbeddingSection,
    val queryEmbedding: List<Double>,
    val score: Double
)