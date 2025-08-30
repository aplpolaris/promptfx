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

class PromptLibrary_LoadTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun testLoadingPromptResourcesWithArguments() {
        // Load the library which includes the resource files we've updated
        val library = PromptLibrary().apply {
            addGroup(PromptIO.readFromResource("examples.yaml"))
            addGroup(PromptIO.readFromResource("text-translate.yaml"))
            addGroup(PromptIO.readFromResource("text-summarize.yaml"))
        }

        // Test that some of the prompts we updated have the argument metadata
        val examplePrompt = library.get("examples/hello-world@1.0.0")
        assertNotNull(examplePrompt, "Hello world example prompt should exist")
        assertEquals(1, examplePrompt!!.args.size)
        assertEquals("input", examplePrompt.args[0].name)
        assertEquals("The input text to include in the example", examplePrompt.args[0].description)
        assertTrue(examplePrompt.args[0].required)

        val translatePrompt = library.get("text-translate/translate@1.0.0")
        assertNotNull(translatePrompt, "Translate prompt should exist")
        assertEquals(2, translatePrompt!!.args.size)

        val inputArg = translatePrompt.args.find { it.name == "input" }
        val instructArg = translatePrompt.args.find { it.name == "instruct" }
        assertNotNull(inputArg)
        assertNotNull(instructArg)
        assertEquals("The text to be translated", inputArg!!.description)
        assertEquals("The target language for translation", instructArg!!.description)
        assertTrue(inputArg.required)
        assertTrue(instructArg.required)

        val summarizePrompt = library.get("text-summarize/summarize@1.0.0")
        assertNotNull(summarizePrompt, "Summarize prompt should exist")
        assertEquals(4, summarizePrompt!!.args.size)

        val requiredArgs = summarizePrompt.args.filter { it.required }
        val optionalArgs = summarizePrompt.args.filter { !it.required }
        assertEquals(1, requiredArgs.size) // Only input should be required
        assertEquals(3, optionalArgs.size) // audience, style, format should be optional
        assertEquals("input", requiredArgs[0].name)
    }

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

        val library = PromptLibrary.Companion.loadFromPath(promptFile.toString())
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

    @Test
    fun testLoadDefaultPromptLibrary_with_filter() {
        // Test that PromptLibrary can be created with custom config
        val filter = PromptFilter(
            includeCategories = listOf("text")
        )

        val library = PromptLibrary.loadDefaultPromptLibrary(filter)
        assertNotNull(library)

        // The library should only contain prompts from text category
        val prompts = library.list()
        assertTrue(prompts.all { it.category == "text" || it.category == null })
    }

}
