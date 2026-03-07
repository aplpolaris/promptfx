/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.prompt

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class PromptLibraryFilterTest {

    private val CHAT_PROMPT = PromptDef(id = "chat/chat-back@1.0.0", category = "chat")
    private val TEXT_PROMPT = PromptDef(id = "text-summarize/summarize@1.0.0", category = "text")
    private val EXAMPLE_PROMPT = PromptDef(id = "examples/color@1.0.0", category = "examples")
    private val NO_CAT_PROMPT = PromptDef(id = "misc/tool@1.0.0")

    // ---- isAcceptAll ----

    @Test
    fun `empty filter isAcceptAll`() {
        assertTrue(PromptLibraryFilter().isAcceptAll)
    }

    @Test
    fun `filter with patterns is not isAcceptAll`() {
        assertFalse(PromptLibraryFilter(includeIds = listOf("chat/*")).isAcceptAll)
        assertFalse(PromptLibraryFilter(excludeCategories = listOf("examples")).isAcceptAll)
    }

    // ---- accepts with no patterns ----

    @Test
    fun `empty filter accepts all prompts`() {
        val filter = PromptLibraryFilter()
        assertTrue(filter.accepts(CHAT_PROMPT))
        assertTrue(filter.accepts(TEXT_PROMPT))
        assertTrue(filter.accepts(EXAMPLE_PROMPT))
        assertTrue(filter.accepts(NO_CAT_PROMPT))
    }

    // ---- includeIds ----

    @Test
    fun `includeIds exact prefix match`() {
        val filter = PromptLibraryFilter(includeIds = listOf("chat/*"))
        assertTrue(filter.accepts(CHAT_PROMPT))
        assertFalse(filter.accepts(TEXT_PROMPT))
        assertFalse(filter.accepts(EXAMPLE_PROMPT))
    }

    @Test
    fun `includeIds wildcard star`() {
        val filter = PromptLibraryFilter(includeIds = listOf("text-*"))
        assertTrue(filter.accepts(TEXT_PROMPT))
        assertFalse(filter.accepts(CHAT_PROMPT))
    }

    @Test
    fun `includeIds multiple patterns`() {
        val filter = PromptLibraryFilter(includeIds = listOf("chat/*", "text-*"))
        assertTrue(filter.accepts(CHAT_PROMPT))
        assertTrue(filter.accepts(TEXT_PROMPT))
        assertFalse(filter.accepts(EXAMPLE_PROMPT))
    }

    // ---- includeCategories ----

    @Test
    fun `includeCategories exact match`() {
        val filter = PromptLibraryFilter(includeCategories = listOf("text"))
        assertTrue(filter.accepts(TEXT_PROMPT))
        assertFalse(filter.accepts(CHAT_PROMPT))
    }

    @Test
    fun `includeCategories wildcard`() {
        val filter = PromptLibraryFilter(includeCategories = listOf("text*"))
        assertTrue(filter.accepts(TEXT_PROMPT))
        assertFalse(filter.accepts(CHAT_PROMPT))
    }

    @Test
    fun `includeCategories does not match prompt without category`() {
        val filter = PromptLibraryFilter(includeCategories = listOf("text"))
        assertFalse(filter.accepts(NO_CAT_PROMPT))
    }

    @Test
    fun `includeIds OR includeCategories - either match passes`() {
        val filter = PromptLibraryFilter(includeIds = listOf("chat/*"), includeCategories = listOf("text"))
        assertTrue(filter.accepts(CHAT_PROMPT))
        assertTrue(filter.accepts(TEXT_PROMPT))
        assertFalse(filter.accepts(EXAMPLE_PROMPT))
    }

    // ---- excludeIds ----

    @Test
    fun `excludeIds removes matching prompts`() {
        val filter = PromptLibraryFilter(excludeIds = listOf("examples/*"))
        assertTrue(filter.accepts(CHAT_PROMPT))
        assertTrue(filter.accepts(TEXT_PROMPT))
        assertFalse(filter.accepts(EXAMPLE_PROMPT))
    }

    @Test
    fun `excludeIds applied after includeIds`() {
        val filter = PromptLibraryFilter(
            includeCategories = listOf("chat", "text", "examples"),
            excludeIds = listOf("examples/*")
        )
        assertTrue(filter.accepts(CHAT_PROMPT))
        assertTrue(filter.accepts(TEXT_PROMPT))
        assertFalse(filter.accepts(EXAMPLE_PROMPT))
    }

    // ---- excludeCategories ----

    @Test
    fun `excludeCategories removes matching prompts`() {
        val filter = PromptLibraryFilter(excludeCategories = listOf("examples"))
        assertTrue(filter.accepts(CHAT_PROMPT))
        assertTrue(filter.accepts(TEXT_PROMPT))
        assertFalse(filter.accepts(EXAMPLE_PROMPT))
    }

    @Test
    fun `excludeCategories does not affect prompt without category`() {
        val filter = PromptLibraryFilter(excludeCategories = listOf("examples"))
        assertTrue(filter.accepts(NO_CAT_PROMPT))
    }

    // ---- case-insensitive ----

    @Test
    fun `pattern matching is case-insensitive`() {
        val filter = PromptLibraryFilter(includeCategories = listOf("TEXT"))
        assertTrue(filter.accepts(TEXT_PROMPT))

        val filter2 = PromptLibraryFilter(excludeIds = listOf("EXAMPLES/*"))
        assertFalse(filter2.accepts(EXAMPLE_PROMPT))
    }

    // ---- question mark wildcard ----

    @Test
    fun `question mark wildcard matches single character`() {
        val filter = PromptLibraryFilter(includeCategories = listOf("tex?"))
        assertTrue(filter.accepts(TEXT_PROMPT))
        assertFalse(filter.accepts(CHAT_PROMPT))
    }

    // ---- loading from resource ----

    @Test
    fun `load returns accept-all filter from default resource`() {
        // The built-in prompt-library.yaml has empty lists, so isAcceptAll should be true
        val filter = PromptLibraryFilter.load()
        assertTrue(filter.isAcceptAll, "Default built-in filter should accept all prompts")
    }

    // ---- loading from file ----

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `loadFromFileOrDefault reads specified file`() {
        val filterFile = tempDir.resolve("my-filter.yaml")
        Files.writeString(filterFile, """
            includeCategories:
              - text
            excludeIds:
              - "examples/*"
        """.trimIndent())

        val filter = PromptLibraryFilter.loadFromFileOrDefault(filterFile.toString())
        assertEquals(listOf("text"), filter.includeCategories)
        assertEquals(listOf("examples/*"), filter.excludeIds)
        assertTrue(filter.includeIds.isEmpty())
        assertTrue(filter.excludeCategories.isEmpty())
    }

    @Test
    fun `loadFromFileOrDefault with missing path falls back to default`() {
        val filter = PromptLibraryFilter.loadFromFileOrDefault("/nonexistent/prompt-library.yaml")
        assertTrue(filter.isAcceptAll, "Should fall back to accept-all default")
    }

    // ---- integration with PromptLibrary ----

    @Test
    fun `PromptLibrary default instance loads prompts (filter is accept-all)`() {
        val library = PromptLibrary.INSTANCE
        val prompts = library.list()
        assertTrue(prompts.isNotEmpty(), "Default library should contain prompts")
    }

    @Test
    fun `PromptLibrary filtered to single category`() {
        val filter = PromptLibraryFilter(includeCategories = listOf("examples"))
        val library = PromptLibrary().apply {
            addGroup(PromptGroupIO.readFromResource("chat.yaml"))
            addGroup(PromptGroupIO.readFromResource("examples.yaml"))
        }
        val prompts = library.list().filter { filter.accepts(it) }
        assertTrue(prompts.isNotEmpty(), "Should have loaded example prompts")
        assertTrue(prompts.all { it.category == "examples" }, "All prompts should be in examples category")
    }
}
