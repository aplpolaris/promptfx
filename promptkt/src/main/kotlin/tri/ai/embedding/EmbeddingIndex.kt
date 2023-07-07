package tri.ai.embedding

import java.io.File

/** An interface for an embedding index. */
interface EmbeddingIndex {
    /** Find the most similar section to the query. */
    suspend fun findMostSimilar(query: String, n: Int): List<EmbeddingMatch>
}

/** A scored match for a query. */
class EmbeddingMatch(
    val document: EmbeddingDocument,
    val section: EmbeddingSection,
    val score: Double
) {
    fun readText() = document.readText(section)

    override fun toString(): String {
        return "${File(document.path).name} (${"%.2f".format(score)}) ${readText().take(500)}"
    }
}