/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.prompt.PromptDef

class PromptFxRuntimeConfigTest {

    //region matchesGlob tests

    @Test
    fun `glob star matches any non-slash sequence`() {
        assertTrue(PromptFxRuntimeConfig.matchesGlob("gpt-4", "*gpt*"))
        assertTrue(PromptFxRuntimeConfig.matchesGlob("gpt-4", "gpt*"))
        assertTrue(PromptFxRuntimeConfig.matchesGlob("gpt-4", "*4"))
        assertFalse(PromptFxRuntimeConfig.matchesGlob("claude-3", "*gpt*"))
    }

    @Test
    fun `glob star does not cross slash`() {
        assertFalse(PromptFxRuntimeConfig.matchesGlob("text/summarize", "*"))
        assertTrue(PromptFxRuntimeConfig.matchesGlob("summarize", "*"))
    }

    @Test
    fun `glob double star crosses slash`() {
        assertTrue(PromptFxRuntimeConfig.matchesGlob("text/summarize", "**"))
        assertTrue(PromptFxRuntimeConfig.matchesGlob("text/summarize", "text/**"))
        assertTrue(PromptFxRuntimeConfig.matchesGlob("text/summarize", "**/summarize"))
    }

    @Test
    fun `glob exact match`() {
        assertTrue(PromptFxRuntimeConfig.matchesGlob("gpt-4", "gpt-4"))
        assertFalse(PromptFxRuntimeConfig.matchesGlob("gpt-4o", "gpt-4"))
    }

    @Test
    fun `glob question mark matches single char`() {
        assertTrue(PromptFxRuntimeConfig.matchesGlob("gpt-4", "gpt-?"))
        assertFalse(PromptFxRuntimeConfig.matchesGlob("gpt-4o", "gpt-?"))
    }

    @Test
    fun `glob path segment matching`() {
        assertTrue(PromptFxRuntimeConfig.matchesGlob("text/summarize", "text/*"))
        assertFalse(PromptFxRuntimeConfig.matchesGlob("code/summarize", "text/*"))
        assertTrue(PromptFxRuntimeConfig.matchesGlob("text/summarize", "*/summarize"))
    }

    //endregion

    //region isPromptActive tests

    private fun prompt(id: String, category: String? = null) = PromptDef(id = id, category = category)

    @Test
    fun `no filters - all prompts active`() {
        val config = PromptFxRuntimeConfig
        val saved = Pair(config.promptIncludePatterns, config.promptExcludePatterns)
        try {
            setPromptFilters(listOf(), listOf())
            assertTrue(config.isPromptActive(prompt("text/summarize")))
            assertTrue(config.isPromptActive(prompt("code/explain")))
        } finally {
            setPromptFilters(saved.first, saved.second)
        }
    }

    @Test
    fun `include filter restricts to matching prompts`() {
        val config = PromptFxRuntimeConfig
        val saved = Pair(config.promptIncludePatterns, config.promptExcludePatterns)
        try {
            setPromptFilters(listOf("text/*"), listOf())
            assertTrue(config.isPromptActive(prompt("text/summarize")))
            assertFalse(config.isPromptActive(prompt("code/explain")))
        } finally {
            setPromptFilters(saved.first, saved.second)
        }
    }

    @Test
    fun `exclude filter removes matching prompts`() {
        val config = PromptFxRuntimeConfig
        val saved = Pair(config.promptIncludePatterns, config.promptExcludePatterns)
        try {
            setPromptFilters(listOf(), listOf("code/*"))
            assertTrue(config.isPromptActive(prompt("text/summarize")))
            assertFalse(config.isPromptActive(prompt("code/explain")))
        } finally {
            setPromptFilters(saved.first, saved.second)
        }
    }

    @Test
    fun `exclude overrides include for matching prompts`() {
        val config = PromptFxRuntimeConfig
        val saved = Pair(config.promptIncludePatterns, config.promptExcludePatterns)
        try {
            setPromptFilters(listOf("**"), listOf("code/*"))
            assertTrue(config.isPromptActive(prompt("text/summarize")))
            assertFalse(config.isPromptActive(prompt("code/explain")))
        } finally {
            setPromptFilters(saved.first, saved.second)
        }
    }

    @Test
    fun `prompt filter matches against category as well as id`() {
        val config = PromptFxRuntimeConfig
        val saved = Pair(config.promptIncludePatterns, config.promptExcludePatterns)
        try {
            setPromptFilters(listOf(), listOf("text"))
            // id doesn't match, but category does - should be excluded
            assertFalse(config.isPromptActive(prompt("text/summarize", category = "text")))
            // neither id nor category match
            assertTrue(config.isPromptActive(prompt("code/explain", category = "code")))
        } finally {
            setPromptFilters(saved.first, saved.second)
        }
    }

    //endregion

    //region isModelActive tests

    @Test
    fun `no model filters - all models active`() {
        val config = PromptFxRuntimeConfig
        val saved = Pair(config.modelIncludePatterns, config.modelExcludePatterns)
        try {
            setModelFilters(listOf(), listOf())
            assertTrue(config.isModelActive("gpt-4"))
            assertTrue(config.isModelActive("claude-3"))
        } finally {
            setModelFilters(saved.first, saved.second)
        }
    }

    @Test
    fun `model include filter restricts models`() {
        val config = PromptFxRuntimeConfig
        val saved = Pair(config.modelIncludePatterns, config.modelExcludePatterns)
        try {
            setModelFilters(listOf("gpt*"), listOf())
            assertTrue(config.isModelActive("gpt-4"))
            assertTrue(config.isModelActive("gpt-3.5-turbo"))
            assertFalse(config.isModelActive("claude-3"))
        } finally {
            setModelFilters(saved.first, saved.second)
        }
    }

    @Test
    fun `model exclude filter removes matching models`() {
        val config = PromptFxRuntimeConfig
        val saved = Pair(config.modelIncludePatterns, config.modelExcludePatterns)
        try {
            setModelFilters(listOf(), listOf("*claude*"))
            assertTrue(config.isModelActive("gpt-4"))
            assertFalse(config.isModelActive("claude-3"))
        } finally {
            setModelFilters(saved.first, saved.second)
        }
    }

    //endregion

    // Helpers to set filter state via reflection (since fields are private set)
    private fun setPromptFilters(include: List<String>, exclude: List<String>) {
        val f1 = PromptFxRuntimeConfig::class.java.getDeclaredField("promptIncludePatterns")
        f1.isAccessible = true; f1.set(PromptFxRuntimeConfig, include)
        val f2 = PromptFxRuntimeConfig::class.java.getDeclaredField("promptExcludePatterns")
        f2.isAccessible = true; f2.set(PromptFxRuntimeConfig, exclude)
    }

    private fun setModelFilters(include: List<String>, exclude: List<String>) {
        val f1 = PromptFxRuntimeConfig::class.java.getDeclaredField("modelIncludePatterns")
        f1.isAccessible = true; f1.set(PromptFxRuntimeConfig, include)
        val f2 = PromptFxRuntimeConfig::class.java.getDeclaredField("modelExcludePatterns")
        f2.isAccessible = true; f2.set(PromptFxRuntimeConfig, exclude)
    }
}
