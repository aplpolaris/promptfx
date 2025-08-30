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

class PromptFilterTest {

    private val testPrompt1 = PromptDef(
        id = "text-summarize/basic@1.0.0",
        category = "text",
        template = "Summarize: {{input}}"
    )
    
    private val testPrompt2 = PromptDef(
        id = "docs-qa/simple@1.0.0", 
        category = "docs",
        template = "Answer: {{question}}"
    )
    
    private val testPrompt3 = PromptDef(
        id = "experimental/beta-feature@1.0.0",
        category = "experimental", 
        template = "Test: {{input}}"
    )

    @Test
    fun testDefaultConfigIncludesAll() {
        val filter = PromptFilter.ACCEPT_ALL
        
        assertTrue(filter(testPrompt1))
        assertTrue(filter(testPrompt2))
        assertTrue(filter(testPrompt3))
    }

    @Test
    fun testIncludeIdPatterns() {
        val filter = PromptFilter(
            includeIds = listOf("text-*", "docs-qa/*")
        )
        
        assertTrue(filter(testPrompt1))
        assertTrue(filter(testPrompt2))
        assertFalse(filter(testPrompt3))
    }

    @Test
    fun testIncludeCategoryPatterns() {
        val filter = PromptFilter(
            includeCategories = listOf("text", "docs")
        )
        
        assertTrue(filter(testPrompt1))
        assertTrue(filter(testPrompt2))
        assertFalse(filter(testPrompt3))
    }

    @Test
    fun testExcludeIdPatterns() {
        val filter = PromptFilter(
            excludeIds = listOf("*beta*", "experimental/*")
        )
        
        assertTrue(filter(testPrompt1))
        assertTrue(filter(testPrompt2))
        assertFalse(filter(testPrompt3))
    }

    @Test
    fun testExcludeCategoryPatterns() {
        val filter = PromptFilter(
            excludeCategories = listOf("experimental")
        )
        
        assertTrue(filter(testPrompt1))
        assertTrue(filter(testPrompt2))
        assertFalse(filter(testPrompt3))
    }

    @Test
    fun testCombinedIncludeAndExclude() {
        val filter = PromptFilter(
            includeCategories = listOf("*"), // Include all categories
            excludeIds = listOf("*beta*") // But exclude beta features
        )
        
        assertTrue(filter(testPrompt1))
        assertTrue(filter(testPrompt2))
        assertFalse(filter(testPrompt3))
    }

    @Test
    fun testWildcardPatterns() {
        val filter = PromptFilter(
            includeIds = listOf("*")
        )
        
        assertTrue(filter(testPrompt1))
        assertTrue(filter(testPrompt2))
        assertTrue(filter(testPrompt3))
    }

    @Test
    fun testExactMatch() {
        val filter = PromptFilter(
            includeIds = listOf("text-summarize/basic@1.0.0")
        )
        
        assertTrue(filter(testPrompt1))
        assertFalse(filter(testPrompt2))
        assertFalse(filter(testPrompt3))
    }

    @Test
    fun testPatternMatching() {
        val filter = PromptFilter()
        
        // Test private matchesPattern method via shouldInclude
        val testPromptA = PromptDef(id = "test-abc", template = "test")
        val testPromptB = PromptDef(id = "test-xyz", template = "test")
        val testPromptC = PromptDef(id = "other-abc", template = "test")
        
        val configPattern = PromptFilter(includeIds = listOf("test-*"))
        
        assertTrue(configPattern(testPromptA))
        assertTrue(configPattern(testPromptB))
        assertFalse(configPattern(testPromptC))
    }
}