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
import java.nio.file.Files
import java.nio.file.Path

class PromptLibraryConfigIOTest {

    @Test
    fun testLoadDefaultConfigFromResource() {
        val config = PromptGroupIO.loadConfigFromResource("config/prompt-library-config.yaml")
        
        assertNotNull(config)
        assertTrue(config.includeIds.isEmpty())
        assertTrue(config.includeCategories.isEmpty())
        assertTrue(config.excludeIds.isEmpty())
        assertTrue(config.excludeCategories.isEmpty())
    }

    @Test
    fun testLoadConfigFromNonExistentResource() {
        val config = PromptGroupIO.loadConfigFromResource("non-existent-config.yaml")
        
        assertEquals(PromptLibraryConfig.DEFAULT, config)
    }

    @Test
    fun testLoadConfigFromFile() {
        // Create a temporary config file
        val tempFile = Files.createTempFile("test-config", ".yaml")
        val configContent = """
            includeIds:
              - "text-*"
              - "docs-*"
            includeCategories:
              - "text"
            excludeIds:
              - "*beta*"
            excludeCategories:
              - "experimental"
        """.trimIndent()
        
        Files.write(tempFile, configContent.toByteArray())
        
        try {
            val config = PromptGroupIO.loadConfigFromFile(tempFile)
            
            assertEquals(listOf("text-*", "docs-*"), config.includeIds)
            assertEquals(listOf("text"), config.includeCategories)
            assertEquals(listOf("*beta*"), config.excludeIds)
            assertEquals(listOf("experimental"), config.excludeCategories)
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    @Test
    fun testLoadConfigFromNonExistentFile() {
        val nonExistentPath = Path.of("/tmp/non-existent-config.yaml")
        val config = PromptGroupIO.loadConfigFromFile(nonExistentPath)
        
        assertEquals(PromptLibraryConfig.DEFAULT, config)
    }

    @Test
    fun testPromptLibraryWithConfig() {
        // Test that PromptLibrary can be created with custom config
        val config = PromptLibraryConfig(
            includeCategories = listOf("text")
        )
        
        val library = PromptLibrary.loadWithConfig(config)
        assertNotNull(library)
        
        // The library should only contain prompts from text category
        val prompts = library.list()
        assertTrue(prompts.all { it.category == "text" || it.category == null })
    }
}