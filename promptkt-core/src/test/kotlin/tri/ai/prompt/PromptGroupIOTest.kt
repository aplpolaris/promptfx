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

class PromptGroupIOTest {

    @Test
    fun testPromptExists() {
        val group = PromptGroupIO.readFromResource("chat.yaml")
        assertEquals("chat", group.groupId) { "Prompt group ID should be 'chat'" }
        assert(group.prompts.isNotEmpty()) { "Prompt group should not be empty" }
        assertEquals("chat", group.defaults.category)
        assertEquals("chat-back", group.prompts[0].name)
    }

    @Test
    fun testResolved() {
        val group = PromptGroupIO.readFromResource("chat.yaml")
        val prompt = group.prompts[0]
        assertEquals("chat/chat-back@1.0.0", prompt.id)
        assertEquals("chat-back", prompt.name)
        assertEquals("chat", prompt.category)
        assertEquals("1.0.0", prompt.version)
    }

    @Test
    fun testReadAll() {
        val groups = PromptGroupIO.readAllFromResourceDirectory()
        assertEquals(20, groups.size)
    }

    @Test
    fun testLoadingPromptResourcesWithArguments() {
        // Load the library which includes the resource files we've updated
        val library = PromptLibrary().apply {
            addGroup(PromptGroupIO.readFromResource("examples.yaml"))
            addGroup(PromptGroupIO.readFromResource("text-translate.yaml"))
            addGroup(PromptGroupIO.readFromResource("text-summarize.yaml"))
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
    fun testPromptResourcesCanFillTemplates() {
        val library = PromptLibrary().apply {
            addGroup(PromptGroupIO.readFromResource("text-translate.yaml"))
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

}
