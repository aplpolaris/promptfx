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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class PromptLibraryTest {

    val TEST_LIB = PromptLibrary().apply {
        addGroup(PromptGroupIO.readFromResource("chat.yaml"))
        addGroup(PromptGroupIO.readFromResource("examples.yaml"))
    }

    @Test
    fun testGet() {
        assertNotNull(TEST_LIB.get("chat/chat-back@1.0.0"))
        assertNotNull(TEST_LIB.get("chat/chat-back"))
        assertNull(TEST_LIB.get("chat-back"))
    }

    @Test
    fun testPrint() {
        println("---- TEST_LIB ----")
        TEST_LIB.printAll()
        println("---- PROMPT LIBRARY (DEFAULT) ----")
        PromptLibrary.INSTANCE.printAll()
    }

    // print all prompts in the library, organized by category then sorted by id
    private fun PromptLibrary.printAll() {
        val all = list()
            .groupBy { it.category ?: "Uncategorized" }
            .toSortedMap()
        all.forEach { (category, prompts) ->
            println("$category/")
            prompts.sortedBy { it.id }.forEach { prompt ->
                // print tabs with id, spaced 30 chars to version, spaced 10 chars to title
                println("\t${prompt.name!!.padEnd(30)} @${(prompt.version ?: "n/a").padEnd(10)} ${prompt.title ?: ""}")
            }
        }
    }

    @Test
    fun testList_by_category() {
        val list = TEST_LIB.list(category = "examples")
        println(list.joinToString { it.id })
        assertEquals(3, list.size)
    }

    @Test
    fun testList_by_tag() {
        val list = TEST_LIB.list(tag = "color")
        println(list.joinToString { it.id })
        assertEquals(1, list.size)

        assertEquals(0, TEST_LIB.list(category = "chat", tag = "color").size)
    }

    @Test
    fun testList_by_prefix() {
        val list = TEST_LIB.list(prefix = "examples/")
        println(list.joinToString { it.id })
        assertEquals(3, list.size)

        val list2 = TEST_LIB.list(prefix = "examples/color")
        println(list2.joinToString { it.id })
        assertEquals(1, list2.size)
    }

    @Test
    fun testPromptFill() {
        val prompt = PromptLibrary.INSTANCE.get("text-qa/answer")
        val result = prompt!!.template().fillInstruct(
            input = "42",
            instruct = "What is the meaning of life?"
        )
        println(result)
        Assertions.assertEquals(161, result.length)
    }
    @Test
    fun testTemplateMetadataCompatibility() {
        val prompt = PromptLibrary.INSTANCE.get("text-qa/answer")
        assertNotNull(prompt)
        assertEquals("answer", prompt!!.name)
        assertNotNull(prompt.template)
        assertTrue(prompt.template!!.isNotBlank())
    }

    @Test
    fun testPromptWrite() {
        val prompt = PromptDef(id = "a template", description = "description", name = "name", template = "")
        assertEquals("""
            ---
            id: a template
            name: name
            description: description
            
        """.trimIndent(), PromptGroupIO.MAPPER.writeValueAsString(prompt))
    }

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
