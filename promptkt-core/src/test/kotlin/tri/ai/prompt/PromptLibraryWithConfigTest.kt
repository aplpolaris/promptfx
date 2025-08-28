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
package tri.ai.prompt

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PromptLibraryWithConfigTest {

    @Test
    fun testDefaultLibraryLoadsAllPrompts() {
        val library = PromptLibrary.loadWithConfig()
        val prompts = library.list()
        
        assertTrue(prompts.isNotEmpty())
        assertTrue(prompts.any { it.category?.startsWith("text-") == true })
        assertTrue(prompts.any { it.category?.startsWith("docs-") == true })
        assertTrue(prompts.any { it.category == "examples" })
    }

    @Test 
    fun testLibraryWithTextCategoryFilter() {
        val config = PromptLibraryConfig(includeCategories = listOf("text-*"))
        val library = PromptLibrary.loadWithConfig(config)
        val prompts = library.list()
        
        assertTrue(prompts.isNotEmpty())
        assertTrue(prompts.all { it.category?.startsWith("text-") == true })
        assertFalse(prompts.any { it.category?.startsWith("docs-") == true })
        assertFalse(prompts.any { it.category == "examples" })
    }

    @Test 
    fun testLibraryWithExcludeExamplesFilter() {
        val config = PromptLibraryConfig(excludeCategories = listOf("examples"))
        val library = PromptLibrary.loadWithConfig(config)
        val prompts = library.list()
        
        assertTrue(prompts.isNotEmpty())
        assertFalse(prompts.any { it.category == "examples" })
        assertTrue(prompts.any { it.category?.startsWith("text-") == true })
        assertTrue(prompts.any { it.category?.startsWith("docs-") == true })
    }

    @Test 
    fun testLibraryWithIdPatternFilter() {
        val config = PromptLibraryConfig(includeIds = listOf("text-*"))
        val library = PromptLibrary.loadWithConfig(config)
        val prompts = library.list()
        
        assertTrue(prompts.isNotEmpty())
        assertTrue(prompts.all { it.id.startsWith("text-") })
    }

    @Test 
    fun testLibraryWithCombinedFilters() {
        val config = PromptLibraryConfig(
            includeCategories = listOf("*"), // Include all
            excludeCategories = listOf("examples", "docs-*") // But exclude these
        )
        val library = PromptLibrary.loadWithConfig(config)
        val prompts = library.list()
        
        assertTrue(prompts.isNotEmpty())
        assertFalse(prompts.any { it.category == "examples" })
        assertFalse(prompts.any { it.category?.startsWith("docs-") == true })
        assertTrue(prompts.any { it.category?.startsWith("text-") == true })
    }
}