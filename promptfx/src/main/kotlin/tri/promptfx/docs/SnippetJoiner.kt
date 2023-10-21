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
package tri.promptfx.docs

import tri.ai.prompt.AiPromptLibrary

/**
 * Strategy for constructing a context from a set of matches.
 * This may rearrange the snippets and/or construct the concatenated text for an LLM query.
 */
sealed class SnippetJoiner(val id: String) {
    /** Constructs the context from the given matches. */
    abstract fun constructContext(matches: List<SnippetMatch>): String
}

class BasicTemplateJoiner(_id: String) : SnippetJoiner(_id) {
    override fun constructContext(matches: List<SnippetMatch>) =
        AiPromptLibrary.lookupPrompt(id)
            .fill("matches" to matches.mapIndexed { i, it ->
                NameText(i + 1, it)
            })
}

class GroupingTemplateJoiner(_id: String) : SnippetJoiner(_id) {
    override fun constructContext(matches: List<SnippetMatch>) =
        matches.groupBy { it.embeddingMatch.document.shortNameWithoutExtension }.mapValues {
            it.value.joinToString("\n...\n") { it.snippetText.trim() }
        }.let {
            AiPromptLibrary.lookupPrompt(id)
                .fill("matches" to it.entries.mapIndexed { i, it -> NameText(i + 1, it.key, it.value) })
        }
}

private class NameText(val number: Int, val name: String, val text: String) {
    constructor(index: Int, match: SnippetMatch) : this(index, match.embeddingMatch.document.shortNameWithoutExtension, match.snippetText.trim())
}