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

class PromptLibraryConfigTest {

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
        val config = PromptLibraryConfig.DEFAULT
        
        assertTrue(config.shouldInclude(testPrompt1))
        assertTrue(config.shouldInclude(testPrompt2))
        assertTrue(config.shouldInclude(testPrompt3))
    }

    @Test
    fun testIncludeIdPatterns() {
        val config = PromptLibraryConfig(
            includeIds = listOf("text-*", "docs-qa/*")
        )
        
        assertTrue(config.shouldInclude(testPrompt1))
        assertTrue(config.shouldInclude(testPrompt2))
        assertFalse(config.shouldInclude(testPrompt3))
    }

    @Test
    fun testIncludeCategoryPatterns() {
        val config = PromptLibraryConfig(
            includeCategories = listOf("text", "docs")
        )
        
        assertTrue(config.shouldInclude(testPrompt1))
        assertTrue(config.shouldInclude(testPrompt2))
        assertFalse(config.shouldInclude(testPrompt3))
    }

    @Test
    fun testExcludeIdPatterns() {
        val config = PromptLibraryConfig(
            excludeIds = listOf("*beta*", "experimental/*")
        )
        
        assertTrue(config.shouldInclude(testPrompt1))
        assertTrue(config.shouldInclude(testPrompt2))
        assertFalse(config.shouldInclude(testPrompt3))
    }

    @Test
    fun testExcludeCategoryPatterns() {
        val config = PromptLibraryConfig(
            excludeCategories = listOf("experimental")
        )
        
        assertTrue(config.shouldInclude(testPrompt1))
        assertTrue(config.shouldInclude(testPrompt2))
        assertFalse(config.shouldInclude(testPrompt3))
    }

    @Test
    fun testCombinedIncludeAndExclude() {
        val config = PromptLibraryConfig(
            includeCategories = listOf("*"), // Include all categories
            excludeIds = listOf("*beta*") // But exclude beta features
        )
        
        assertTrue(config.shouldInclude(testPrompt1))
        assertTrue(config.shouldInclude(testPrompt2))
        assertFalse(config.shouldInclude(testPrompt3))
    }

    @Test
    fun testWildcardPatterns() {
        val config = PromptLibraryConfig(
            includeIds = listOf("*")
        )
        
        assertTrue(config.shouldInclude(testPrompt1))
        assertTrue(config.shouldInclude(testPrompt2))
        assertTrue(config.shouldInclude(testPrompt3))
    }

    @Test
    fun testExactMatch() {
        val config = PromptLibraryConfig(
            includeIds = listOf("text-summarize/basic@1.0.0")
        )
        
        assertTrue(config.shouldInclude(testPrompt1))
        assertFalse(config.shouldInclude(testPrompt2))
        assertFalse(config.shouldInclude(testPrompt3))
    }

    @Test
    fun testPatternMatching() {
        val config = PromptLibraryConfig()
        
        // Test private matchesPattern method via shouldInclude
        val testPromptA = PromptDef(id = "test-abc", template = "test")
        val testPromptB = PromptDef(id = "test-xyz", template = "test")
        val testPromptC = PromptDef(id = "other-abc", template = "test")
        
        val configPattern = PromptLibraryConfig(includeIds = listOf("test-*"))
        
        assertTrue(configPattern.shouldInclude(testPromptA))
        assertTrue(configPattern.shouldInclude(testPromptB))
        assertFalse(configPattern.shouldInclude(testPromptC))
    }
}