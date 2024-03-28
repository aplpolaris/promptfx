package tri.ai.embedding

/** A semantic query for a given embedding model. */
class SemanticTextQuery(
    val query: String,
    val embedding: List<Double>,
    val modelId: String
)