package tri.ai.embedding

import java.text.BreakIterator

/** Utilities for breaking up text into chunks. */
object TextChunker {

    /** Basic chunking by splitting on whitespace. */
    fun chunkTextBySectionsSimple(text: String, maxChunkSize: Int): List<Pair<IntRange, String>> {
        val sections = mutableListOf<Pair<IntRange, String>>()
        val words = text.split(Regex("\\s+"))
        val currentSection = StringBuilder()
        var currentIndex = 0
        var currentRangeStart = 0

        for (word in words) {
            if (currentSection.length + word.length + 1 <= maxChunkSize) {
                if (currentSection.isNotEmpty()) {
                    currentSection.append(' ')
                }
                currentSection.append(word)
                currentIndex += word.length + 1
            } else {
                sections.add(Pair(currentRangeStart until currentIndex, currentSection.toString()))

                currentSection.clear()
                currentSection.append(word)
                currentRangeStart = currentIndex + 1
                currentIndex += word.length + 1
            }
        }

        if (currentSection.isNotEmpty()) {
            sections.add(Pair(currentRangeStart until currentIndex, currentSection.toString()))
        }
        return sections
    }

    /** Chunk into sections by section breaks. Optionally combine shorter sections. */
    fun TextChunk.chunkBySections(maxChunkSize: Int, combineShortSections: Boolean): List<TextChunk> {
        // return chunk if it's short enough
        if (combineShortSections && text.length <= maxChunkSize)
            return listOf(this)

        // break into sections and optionally concatenate short sections
        val sections = splitOnSections()
            .recombine(if (combineShortSections) maxChunkSize else 0)

        // split up any sections that are too long
        val result = mutableListOf<TextChunk>()
        sections.forEach { section ->
            if (section.text.length <= maxChunkSize) {
                result += section
            } else {
                result += section.chunkByParagraphs(maxChunkSize)
            }
        }
        return result
    }

    fun TextChunk.chunkByParagraphs(maxChunkSize: Int): List<TextChunk> {
        // return chunk if it's short enough
        if (text.length <= maxChunkSize)
            return listOf(this)

        // break into paragraphs and concatenate short paragraphs
        val paragraphs = splitOnParagraphs()
            .recombine(maxChunkSize)

        // split up any paragraphs that are too long
        val result = mutableListOf<TextChunk>()
        paragraphs.forEach { section ->
            if (section.text.length <= maxChunkSize) {
                result += section
            } else {
                result += section.splitOnSentences()
                    .recombine(maxChunkSize)
            }
        }
        return result
    }

    // TODO - make this find things like likely section headings
    private fun TextChunk.splitOnSections() =
        chunkByDividers(listOf("\n\n\n", "\r\n\r\n\r\n", "\r\r\r", "\n\n", "\r\n\r\n", "\r\r"))

    private fun TextChunk.splitOnParagraphs() =
        chunkByDividers(listOf("\n", "\r\n", "\r"))

    private fun TextChunk.splitOnSentences(): List<TextChunk> {
        val sentences = mutableListOf<TextChunk>()
        val iterator = BreakIterator.getSentenceInstance()
        iterator.setText(text)

        var start = iterator.first()
        var end = iterator.next()

        while (end != BreakIterator.DONE) {
            val sentence = text.substring(start, end).trim()
            if (sentence.isNotEmpty()) {
                sentences.add(TextChunk((range.first + start) until (range.first + end), sentence))
            }
            start = end
            end = iterator.next()
        }

        return sentences
    }

    private fun TextChunk.chunkByDividers(dividers: List<String>): List<TextChunk> {
        val pattern = dividers.joinToString(separator = "|") { Regex.escape(it) }
        val regex = Regex(pattern)
        val chunks = mutableListOf<TextChunk>()
        var currentIndex = 0

        regex.findAll(text).forEach { matchResult ->
            val chunkEnd = matchResult.range.first
            if (chunkEnd > currentIndex) {
                chunks.add(
                    TextChunk(
                    (range.first + currentIndex) .. (range.first + chunkEnd),
                    text.substring(currentIndex, chunkEnd).trim()
                )
                )
            }
            currentIndex = matchResult.range.last + 1
        }

        if (currentIndex < text.length) {
            chunks.add(
                TextChunk(
                (range.first + currentIndex) until (range.first + text.length),
                text.substring(currentIndex).trim()
            )
            )
        }

        return chunks.filter { it.text.isNotBlank() }
    }

}

/** A chunk of text. */
data class TextChunk(val range: IntRange, val text: String) {

    constructor(text: String) : this(text.indices, text)

    val length
        get() = text.length
}

/** Recombines shorter chunks into longer chunks. Assumes list of contiguous chunks. */
fun List<TextChunk>.recombine(maxChunkSize: Int) =
    chunkWhile { it.totalSize() <= maxChunkSize }
        .map { it.concatenate() }

/** Total number of characters in chunks. Assumes list of contiguous chunks. */
fun List<TextChunk>.totalSize() = last().range.last - first().range.first + 1

/** Concatenates all chunks into one. Assumes list of contiguous chunks. */
fun List<TextChunk>.concatenate() = TextChunk(
    first().range.first .. last().range.last,
    joinToString(" ") { it.text }
)

/**
 * Chunks a list into sections, each of which are either size 1, or are the largest sublist for which the given predicate is true.
 * Example: [1, 2, 3, 4, 5, 6, 7] with op sum <= 6 -> [1, 2, 3] | [4] | [5] | [6] | [7]
 */
fun <X> List<X>.chunkWhile(op: (List<X>) -> Boolean): List<List<X>> {
    val result = mutableListOf<List<X>>()
    var current = mutableListOf<X>()
    for (item in this) {
        current.add(item)
        if (!op(current)) {
            if (current.size == 1) {
                result.add(current)
                current = mutableListOf()
            } else {
                result.add(current.dropLast(1))
                current = current.takeLast(1).toMutableList()
            }
        }
    }
    if (current.isNotEmpty()) {
        result.add(current)
    }
    return result
}