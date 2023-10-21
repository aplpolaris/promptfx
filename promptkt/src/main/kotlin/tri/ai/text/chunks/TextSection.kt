package tri.ai.text.chunks

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude

/** A text chunk that is a contiguous section of a larger document. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class TextSection(
    @get:JsonIgnore
    val doc: TextDocument,
    @get:JsonIgnore
    val range: IntRange
) : TextChunk() {

    constructor(doc: TextDocument) : this(doc, doc.text.indices)

    @get:JsonInclude(JsonInclude.Include.ALWAYS)
    val first
        get() = range.first
    @get:JsonInclude(JsonInclude.Include.ALWAYS)
    val last
        get() = range.last

    @get:JsonIgnore
    override val text
        get() = doc.text.substring(range)
    @get: JsonIgnore
    val length
        get() = range.last - range.first
}