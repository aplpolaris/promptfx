package tri.ai.text.chunks

import com.fasterxml.jackson.annotation.JsonIgnore

/** A text chunk that is a contiguous section of a larger document. */
class TextDocumentSection(
    @get:JsonIgnore
    val doc: TextDocument,
    val start: Int,
    val end: Int
) : TextChunk() {

    init {
        require(start in 0..end) { "Invalid start and end: $start, $end" }
    }

    @get:JsonIgnore
    override val text
        get() = doc.text.substring(start, end)
    @get: JsonIgnore
    val length
        get() = end - start
}