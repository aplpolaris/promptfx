package tri.ai.text.chunks.process

import tri.ai.text.chunks.TextChunkRaw

/**
 * Text chunker that splits text by a regex.
 */
class RegexTextChunker(val regex: Regex) : TextChunker {
    override fun chunk(doc: TextChunkRaw) =
        doc.text.split(regex).map { TextChunkRaw(it) }
}

/**
 * Text chunker that splits text on any of given delimiters.
 */
class DelimiterTextChunker(val delimiters: List<String>) : TextChunker {
    override fun chunk(doc: TextChunkRaw) =
        doc.text.split(*delimiters.toTypedArray()).map { TextChunkRaw(it) }
}