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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

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
        assertTrue(prompt.template.isNotBlank())
    }

    @Test
    fun testPromptWrite() {
        val prompt = PromptDef(id = "a template", description = "description", name = "name", template = "")
        assertEquals("""
            ---
            id: "a template"
            name: "name"
            description: "description"
            
        """.trimIndent(), PromptGroupIO.MAPPER.writeValueAsString(prompt))
    }
}
