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
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class PromptLibraryCustomTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun testLoadFromCustomFile() {
        // Create a custom prompt file
        val promptFile = tempDir.resolve("custom-prompts.yaml")
        Files.writeString(
            promptFile, """
            id: test-group
            title: Test Group
            description: Test prompt group for custom loading
            prompts:
              - id: test/custom-prompt
                title: Custom Prompt
                description: A custom test prompt
                template: |
                  This is a custom prompt with input: {{input}}
        """.trimIndent()
        )

        val library = PromptLibrary.loadFromPath(promptFile.toString())
        val prompts = library.list()

        assertTrue(prompts.isNotEmpty(), "Should have loaded at least one prompt")
        val customPrompt = prompts.find { it.id.contains("custom-prompt") }
        assertNotNull(customPrompt, "Should find the custom prompt")
        assertEquals("Custom Prompt", customPrompt!!.title())
        assertEquals("A custom test prompt", customPrompt.description)
    }

    @Test
    fun testLoadFromCustomDirectory() {
        // Create multiple prompt files in a directory
        val promptDir = tempDir.resolve("custom-prompts")
        Files.createDirectories(promptDir)
        
        val promptFile1 = promptDir.resolve("prompts1.yaml")
        Files.writeString(
            promptFile1, """
            id: test-group1
            prompts:
              - id: test/prompt1
                title: Prompt 1
                template: "Prompt 1: {{input}}"
        """.trimIndent()
        )

        val promptFile2 = promptDir.resolve("prompts2.yaml")
        Files.writeString(
            promptFile2, """
            id: test-group2  
            prompts:
              - id: test/prompt2
                title: Prompt 2
                template: "Prompt 2: {{input}}"
        """.trimIndent()
        )

        val library = PromptLibrary.loadFromPath(promptDir.toString())
        val prompts = library.list()

        assertTrue(prompts.size >= 2, "Should have loaded at least 2 prompts")
        assertTrue(prompts.any { it.id.contains("prompt1") }, "Should find prompt1")
        assertTrue(prompts.any { it.id.contains("prompt2") }, "Should find prompt2")
    }

    @Test
    fun testLoadFromNonExistentPath() {
        assertThrows(IllegalArgumentException::class.java) {
            PromptLibrary.loadFromPath("/nonexistent/path")
        }
    }
}