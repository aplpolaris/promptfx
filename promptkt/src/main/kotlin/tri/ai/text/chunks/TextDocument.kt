package tri.ai.text.chunks

/** A text document. */
abstract class TextDocument(id: String): TextChunk() {
    /** Metadata for the document. */
    val metadata = TextMetadata(id)
}

typealias DocumentAttributes = MutableMap<String, Any?>