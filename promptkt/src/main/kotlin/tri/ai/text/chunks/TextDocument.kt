package tri.ai.text.chunks

import com.fasterxml.jackson.annotation.JsonIgnore

/** A text document. */
abstract class TextDocument(id: String): TextChunk() {
    /** Metadata for the document. */
    val metadata = TextMetadata(id)

    /** Get a [TextSection] representing the entire document. */
    @get:JsonIgnore
    val all
        get() = TextSection(this)
}

/** [TextDocument] with a text field. */
class TextDocumentImpl(id: String, override val text: String): TextDocument(id)

/** Alias for document attributes. */
typealias DocumentAttributes = MutableMap<String, Any?>