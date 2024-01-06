package tri.ai.embedding

/** A section with associated document (not for serialization). */
class EmbeddingSectionInDocument(val index: EmbeddingIndex, val doc: EmbeddingDocument, val section: EmbeddingSection) {
    fun readText() = index.readSnippet(doc, section)
}