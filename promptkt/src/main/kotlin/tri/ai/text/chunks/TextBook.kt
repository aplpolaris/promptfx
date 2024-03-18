package tri.ai.text.chunks

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

/**
 * Collection of [TextChunk]s.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class TextBook(id: String? = null, _all: TextChunkRaw? = null) {
    /** Metadata. */
    val metadata = TextBookMetadata(id ?: "")
    /** Additional attributes. */
    val attributes: TextAttributes = mutableMapOf()
    /** Text chunks within this book. */
    val chunks = mutableListOf<TextChunk>()

    /** Optional chunk representation of the entire book contents. */
    val all: TextChunkRaw? = _all

    /** Construct a [TextBook] with a given text string. */
    constructor(id: String, text: String) : this(id, TextChunkRaw(text))
}

/** Metadata for [TextBook]. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class TextBookMetadata(
    var id: String,
    var title: String? = null,
    var author: String? = null,
    var date: LocalDate? = null,
    var path: String? = null,
    var relativePath: String? = null,
)