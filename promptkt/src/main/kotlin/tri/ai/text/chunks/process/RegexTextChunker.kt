/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
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

import tri.ai.text.chunks.TextChunkRaw

/**
 * Text chunker that splits text by a regex.
 * @param cleanUpSpacesFirst if true, clean up spaces before splitting so new lines are standardized to \n and paragraph breaks are always simply \n\n
 * Resulting chunks (between regex matches) are trimmed.
 */
class RegexTextChunker(val cleanUpSpacesFirst: Boolean, val regex: Regex) : TextChunker {
    override fun chunk(doc: TextChunkRaw) =
        doc.text.let { if (cleanUpSpacesFirst) it.cleanUpSpaces() else it }
            .split(regex)
            .filter { it.isNotBlank() }
            .map { it.trim() }
            .map { TextChunkRaw(it) }
}

/**
 * Text chunker that splits text on any of given delimiters.
 * @param cleanUpSpacesFirst if true, clean up spaces before splitting so new lines are standardized to \n and paragraph breaks are always simply \n\n
 */
class DelimiterTextChunker(val cleanUpSpacesFirst: Boolean, val delimiters: List<String>) : TextChunker {
    override fun chunk(doc: TextChunkRaw) =
        doc.text.let { if (cleanUpSpacesFirst) it.cleanUpSpaces() else it }
            .split(*delimiters.toTypedArray())
            .filter { it.isNotBlank() }
            .map { TextChunkRaw(it) }
}

/**
 * Removes any white space before/after line breaks, and standardizes line breaks to \n.
 * Any sequence of 4 or more new lines is reduced to 3 new lines.
 */
private fun String.cleanUpSpaces(): String {
    return replace("\r\n", "\n")
        .replace("\r", "\n")
        .replace("\\n[^\\S\\n]+".toRegex(), "\n")
        .replace("[^\\S\\n]+\\n".toRegex(), "\n")
        .replace("\\n{4,}".toRegex(), "\n\n\n")
}
