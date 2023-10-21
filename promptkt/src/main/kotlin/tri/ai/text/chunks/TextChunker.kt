package tri.ai.text.chunks

/** A text chunker splits a document into chunks. */
interface TextChunker {
    fun chunk(doc: TextDocument): List<TextChunk>
}