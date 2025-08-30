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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class PromptIOTest {

    //region READ/WRITE GROUPS

    @Test
    fun testReadAllFromResourceDirectory() {
        val groups = PromptIO.readAllFromResourceDirectory()
        assertEquals(20, groups.size)
    }

    @Test
    fun testReadFromResources_prompt_exists() {
        val group = PromptIO.readFromResource("chat.yaml")
        assertEquals("chat", group.groupId) { "Prompt group ID should be 'chat'" }
        assert(group.prompts.isNotEmpty()) { "Prompt group should not be empty" }
        assertEquals("chat", group.defaults.category)
        assertEquals("chat-back", group.prompts[0].name)
    }

    @Test
    fun testReadFromResources_resolved() {
        val group = PromptIO.readFromResource("chat.yaml")
        val prompt = group.prompts[0]
        assertEquals("chat/chat-back@1.0.0", prompt.id)
        assertEquals("chat-back", prompt.name)
        assertEquals("chat", prompt.category)
        assertEquals("1.0.0", prompt.version)
    }

    @Test
    fun testReadFromResource_template() {
        val library = PromptLibrary().apply {
            addGroup(PromptIO.readFromResource("text-translate.yaml"))
        }

        val translatePrompt = library.get("text-translate/translate@1.0.0")
        assertNotNull(translatePrompt)

        val result = translatePrompt!!.fill(
            "input" to "Hello, world!",
            "instruct" to "Spanish"
        )

        assertTrue(result.contains("Hello, world!"))
        assertTrue(result.contains("Spanish"))
        assertTrue(result.contains("Translate"))
    }

    //endregion

    //region READ/WRITE TEMPLATES

    @Test
    fun testLoadFilterFromResource_non_existent() {
        val config = PromptIO.loadFilterFromResource("non-existent-config.yaml")
        assertEquals(PromptFilter.ACCEPT_ALL, config)
    }

    @Test
    fun testLoadFilterFromPath() {
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
            val config = PromptIO.loadFilterFromPath(tempFile)

            assertEquals(listOf("text-*", "docs-*"), config.includeIds)
            assertEquals(listOf("text"), config.includeCategories)
            assertEquals(listOf("*beta*"), config.excludeIds)
            assertEquals(listOf("experimental"), config.excludeCategories)
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    @Test
    fun testLoadFilterFromPath_non_existent() {
        val nonExistentPath = Path.of("/tmp/non-existent-config.yaml")
        val config = PromptIO.loadFilterFromPath(nonExistentPath)

        assertEquals(PromptFilter.ACCEPT_ALL, config)
    }

    //endregion

}
