package tri.ai.embedding

import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File

/** A document with a list of sections. */
class EmbeddingDocument(val path: String) {
    /** The sections of the document. */
    val sections: MutableList<EmbeddingSection> = mutableListOf()

    /** Get short name of path. */
    @get:JsonIgnore
    val shortName
        get() = File(path).name

    /** The raw text of the document. */
    fun readText() = File(path).readText()

    /** The raw text of the section. */
    fun readText(section: EmbeddingSection) = readText().substring(section.start, section.end)
}

/** A section of a document. */
class EmbeddingSection(
    val embedding: List<Double>,
    val start: Int,
    val end: Int
) {
    @get: JsonIgnore
    val length
        get() = end - start
}