package tri.ai.text.chunks

import com.fasterxml.jackson.annotation.JsonInclude

/** A [TextChunk] that is a raw text string. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class TextChunkRaw(val text: String) : TextChunk() {
    override fun text(doc: TextChunk?) = text
}