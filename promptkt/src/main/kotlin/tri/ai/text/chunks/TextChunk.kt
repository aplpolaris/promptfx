package tri.ai.text.chunks

/**
 * A text chunk is text with attributes.
 */
abstract class TextChunk {
    /** The raw text of the document. */
    abstract val text: String
    /** Attributes for the document. */
    val attributes: DocumentAttributes = mutableMapOf()
}

