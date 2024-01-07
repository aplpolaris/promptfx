package tri.ai.embedding

import com.fasterxml.jackson.annotation.JsonIgnore

/** A section of a document. */
class EmbeddingSection(
    val embedding: List<Double>,
    val start: Int,
    val end: Int
) {
    @get:JsonIgnore
    val length
        get() = end - start
}