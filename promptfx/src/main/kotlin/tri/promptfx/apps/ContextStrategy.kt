/*-
 * #%L
 * promptfx-0.1.10-SNAPSHOT
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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
package tri.promptfx.apps

/**
 * Strategy for constructing a context from a set of matches.
 * This may rearrange the snippets and/or construct the concatenated text for an LLM query.
 */
sealed class ContextStrategy(val id: String) {
    /** Constructs the context from the given matches. */
    abstract fun constructContext(matches: List<SnippetMatch>): String
}

class ContextStrategyBasic : ContextStrategy("Basic") {
    override fun constructContext(matches: List<SnippetMatch>) =
        matches.joinToString("\n```\n") {
            "Document: ${it.embeddingMatch.document.shortNameWithoutExtension}\n```\nRelevant Text: ${it.snippetText}"
        }
}

class ContextStrategyBasic2 : ContextStrategy("Basic") {
    override fun constructContext(matches: List<SnippetMatch>): String {
        val docs = matches.groupBy { it.embeddingMatch.document.shortNameWithoutExtension }
        return docs.entries.mapIndexed { i, entry ->
            "  - \"\"" + entry.value.joinToString("\n... ") { it.snippetText.trim() } + "\"\"" +
                    " [${i+1}] ${entry.key}"
        }.joinToString("\n  ")
    }
}

class ContextStrategyBasic3 : ContextStrategy("Basic") {
    override fun constructContext(matches: List<SnippetMatch>): String {
        var n = 1
        val docs = matches.groupBy { it.embeddingMatch.document.shortNameWithoutExtension }
        return docs.map { (doc, text) ->
            val combinedText = text.joinToString("\n... ") { it.snippetText.trim() }
            "[[Citation ${n++}]] ${doc}\n\"\"\"\n$combinedText\n\"\"\"\n"
        }.joinToString("\n\n")
    }
}
