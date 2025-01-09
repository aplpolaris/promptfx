/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.ai.text.chunks

import tri.ai.embedding.EmbeddingMatch
import tri.ai.prompt.AiPromptLibrary

/** Name used in snippet joiner template for matching text. */
const val MATCHES_TEMPLATE = "matches"
/** Key used in [TextDoc] attributes for contextual information that will be added to any chunk information returned by the joiner. */
const val TEXT_DOC_ATTRIBUTE_CONTEXT_PREFIX = "context-prefix"

/**
 * Strategy for constructing a context from a set of matches.
 * This may rearrange the snippets and/or construct the concatenated text for an LLM query.
 */
sealed class SnippetJoiner(val id: String) {
    /** Constructs the context from the given matches. */
    abstract fun constructContext(matches: List<EmbeddingMatch>): String
}

/** A basic joiner with content of each chunk in a separate match. */
class BasicTemplateJoiner(_id: String) : SnippetJoiner(_id) {
    override fun constructContext(matches: List<EmbeddingMatch>) =
        AiPromptLibrary.lookupPrompt(id)
            .fill(MATCHES_TEMPLATE to matches.mapIndexed { i, it ->
                NameText(i + 1, it)
            })
}

/** A joiner with content of each chunk grouped by document, along with document metadata. */
class GroupingTemplateJoiner(_id: String) : SnippetJoiner(_id) {
    override fun constructContext(matches: List<EmbeddingMatch>) =
        matches.groupBy { it.shortDocName }.entries.mapIndexed { i, en ->
            val doc = en.value.first().document
            val joinedInThisDoc = en.value.joinToString("\n...\n") { it.chunkText.trim() }.trim()
            val docPrefix = doc.attributes[TEXT_DOC_ATTRIBUTE_CONTEXT_PREFIX] as? String ?: ""
            NameText(i + 1, en.key, docPrefix, joinedInThisDoc)
        }.let {
            AiPromptLibrary.lookupPrompt(id).fill(MATCHES_TEMPLATE to it)
        }
}

/** A joiner with content of each chunk grouped by document, along with document metadata. */
class BulletedTemplateJoiner(_id: String) : SnippetJoiner(_id) {
    override fun constructContext(matches: List<EmbeddingMatch>) =
        constructContextDetailed(matches).context

    /** Detailed response, including a lookup table of chunks by dynamically-generated key. */
    fun constructContextDetailed(matches: List<EmbeddingMatch>): ContextChunkInfo {
        var char = 'A'
        val usedChars = mutableSetOf<Char>()
        val chunkIndex = mutableListOf<ChunkInfo>()
        val fillContent = matches.groupBy { it.shortDocName }.entries.mapIndexed { i, en ->
            val doc = en.value.first().document
            val docChar = if (en.key.length == 1 && en.key[0] !in usedChars) {
                usedChars.add(en.key[0])
                en.key[0]
            } else {
                while (char in usedChars)
                    char++
                usedChars.add(char)
                char
            }
            var num = 1
            val joinedInThisDoc = en.value.joinToString("\n") {
                val key = "$docChar${num++}"
                val chunkText = it.chunkText.trim()
                chunkIndex += ChunkInfo(key, chunkText)
                " - ($key) $chunkText"
            }.trim()
            val docPrefix = doc.attributes[TEXT_DOC_ATTRIBUTE_CONTEXT_PREFIX] as? String ?: ""
            char += 1
            NameText(i + 1, en.key, docPrefix, joinedInThisDoc)
        }
        val filledPrompt = AiPromptLibrary.lookupPrompt(id).fill(MATCHES_TEMPLATE to fillContent)
        return ContextChunkInfo(filledPrompt, chunkIndex)
    }
}

/** Utility class for holding the name and text of a match. This is used when filling in joiner templates with Mustache templates. */
private class NameText(val number: Int, val name: String, val prefix: String, val text: String) {
    constructor(index: Int, match: EmbeddingMatch) : this(index, match.document.browsable()?.shortNameWithoutExtension ?: match.document.metadata.id, "", match.chunkText.trim())
}

/** Utility class for holding collection of chunks related to a context. */
class ContextChunkInfo(val context: String, val chunkIndex: List<ChunkInfo>)

/** Utility class for holding a single chunk. */
class ChunkInfo(val key: String, val text: String)