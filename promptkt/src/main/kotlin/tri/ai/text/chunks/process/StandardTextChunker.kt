/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package tri.ai.text.chunks.process

import tri.ai.text.chunks.TextChunkInDoc
import tri.ai.text.chunks.TextChunk
import tri.ai.text.chunks.TextChunkRaw
import tri.util.info
import java.text.BreakIterator

/** Standard implementation of [TextChunker]. */
class StandardTextChunker(
    val maxChunkSize: Int = 1000
) : TextChunker {

    override fun chunk(doc: TextChunkRaw) =
        doc.chunkBySections(combineShortSections = true)

    //region CODE FROM TextDocumentSectioner.kt

    /** Basic chunking by splitting on whitespace. */
    fun chunkTextBySectionsSimple(text: String): List<Pair<IntRange, String>> {
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

    //endregion

    /** Chunk into sections by section breaks. Optionally combine shorter sections. */
    fun TextChunkRaw.chunkBySections(combineShortSections: Boolean): List<TextChunk> {
        // return chunk if it's short enough
        if (combineShortSections && text.length <= maxChunkSize)
            return listOf(TextChunkInDoc(text.indices))

        // break into sections and optionally concatenate short sections
        val all = TextChunkInDoc(text.indices)
        val sections = all.splitOnSections(this)
            .recombine(if (combineShortSections) maxChunkSize else 0)

        // split up any sections that are too long
        val result = mutableListOf<TextChunk>()
        sections.forEach { section ->
            if (section.text(this).length <= maxChunkSize) {
                result += section
            } else {
                result += section.chunkByParagraphs(this)
            }
        }

        // log chunks
        result.forEach { chunk ->
            info<StandardTextChunker>("  ${(chunk as TextChunkInDoc).range} ${chunk.text(this).firstFiftyChars()}")
        }

        return result
    }

    private fun String.firstFiftyChars() = replace("[\r\n]+".toRegex(), " ").let {
        if (it.length <= 50) it.trim() else (it.substring(0, 50).trim()+"...")
    }

    fun TextChunkInDoc.chunkByParagraphs(doc: TextChunkRaw): List<TextChunkInDoc> {
        // return chunk if it's short enough
        if (text(doc).length <= maxChunkSize)
            return listOf(this)

        // break into paragraphs and concatenate short paragraphs
        val paragraphs = splitOnParagraphs(doc)
            .recombine(maxChunkSize)

        // split up any paragraphs that are too long
        val result = mutableListOf<TextChunkInDoc>()
        paragraphs.forEach { section ->
            if (section.text(doc).length <= maxChunkSize) {
                result += section
            } else {
                result += section.splitOnSentences(doc)
                    .recombine(maxChunkSize)
            }
        }
        return result
    }

    // TODO - make this find things like likely section headings
    private fun TextChunkInDoc.splitOnSections(doc: TextChunkRaw) =
        chunkByDividers(doc, listOf("\n\n\n", "\r\n\r\n\r\n", "\r\r\r", "\n\n", "\r\n\r\n", "\r\r"))

    private fun TextChunkInDoc.splitOnParagraphs(doc: TextChunkRaw) =
        chunkByDividers(doc, listOf("\n", "\r\n", "\r"))

    private fun TextChunkInDoc.splitOnSentences(doc: TextChunkRaw): List<TextChunkInDoc> {
        val sentences = mutableListOf<TextChunkInDoc>()
        val iterator = BreakIterator.getSentenceInstance()
        iterator.setText(text(doc))

        var start = iterator.first()
        var end = iterator.next()

        while (end != BreakIterator.DONE) {
            val sentence = doc.text.substring(start, end).trim()
            if (sentence.isNotEmpty()) {
                sentences.add(TextChunkInDoc(range.first + start, range.first + end - 1))
            }
            start = end
            end = iterator.next()
        }

        return sentences
    }

    private fun TextChunkInDoc.chunkByDividers(doc: TextChunkRaw, dividers: List<String>): List<TextChunk> {
        val pattern = dividers.joinToString(separator = "|") { Regex.escape(it) }
        val regex = Regex(pattern)
        val chunks = mutableListOf<TextChunkInDoc>()
        var currentIndex = 0

        val text = text(doc)
        regex.findAll(text).forEach { matchResult ->
            val chunkEnd = matchResult.range.first
            if (chunkEnd > currentIndex) {
                chunks.add(
                    TextChunkInDoc(range.first + currentIndex, range.first + chunkEnd)
                )
            }
            currentIndex = matchResult.range.last + 1
        }

        if (currentIndex < doc.text.length) {
            chunks.add(
                TextChunkInDoc(range.first + currentIndex, range.first + text.length - 1)
            )
        }

        return chunks.filter { it.text(doc).isNotBlank() }
    }

    //endregion

    companion object {
        /** Recombines shorter chunks into longer chunks. Assumes list of contiguous chunks. */
        private fun List<TextChunk>.recombine(maxChunkSize: Int) =
            chunkWhile { it.totalSize() <= maxChunkSize }
                .map { it.concatenate() }

        /** Total number of characters in chunks. */
        private fun List<TextChunk>.totalSize() = filterIsInstance<TextChunkInDoc>().let {
            val min = it.minOfOrNull { it.range.first } ?: 0
            val max = it.maxOfOrNull { it.range.last } ?: 0
            max - min + 1
        }

        /** Concatenates all chunks into one. */
        private fun List<TextChunk>.concatenate() = filterIsInstance<TextChunkInDoc>().let {
            require(it.isNotEmpty())
            TextChunkInDoc(it.minOfOrNull { it.range.first } ?: 0, it.maxOfOrNull { it.range.last } ?: 0)
        }

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
                    current = if (current.size == 1) {
                        result.add(current)
                        mutableListOf()
                    } else {
                        result.add(current.dropLast(1))
                        current.takeLast(1).toMutableList()
                    }
                }
            }
            if (current.isNotEmpty()) {
                result.add(current)
            }
            return result
        }
    }

}