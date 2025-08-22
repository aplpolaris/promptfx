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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ArgumentMetadataTest {

    @Test
    fun testPromptWithArguments() {
        val prompt = PromptDef(
            id = "test/example@1.0.0",
            title = "Test Prompt with Arguments",
            args = listOf(
                PromptArgDef("input", "The input text", true, PromptArgType.string),
                PromptArgDef("style", "The writing style", false, PromptArgType.string, "casual")
            ),
            template = "Convert the following text to {{style}} style: {{{input}}}"
        )

        // Test argument definitions
        assertEquals(2, prompt.args.size)
        assertEquals("input", prompt.args[0].name)
        assertEquals("The input text", prompt.args[0].description)
        assertTrue(prompt.args[0].required)
        assertEquals(PromptArgType.string, prompt.args[0].type)

        assertEquals("style", prompt.args[1].name)
        assertEquals("The writing style", prompt.args[1].description)
        assertFalse(prompt.args[1].required)
        assertEquals("casual", prompt.args[1].defaultValue)

        // Test template filling
        val result = prompt.fill(
            "input" to "Hello world!",
            "style" to "formal"
        )
        assertEquals("Convert the following text to formal style: Hello world!", result)
    }

    @Test
    fun testEnumerationArgumentType() {
        val prompt = PromptDef(
            id = "test/enum-example@1.0.0",
            title = "Test with Enumeration",
            args = listOf(
                PromptArgDef(
                    name = "sentiment",
                    description = "The sentiment category",
                    required = true,
                    type = PromptArgType.enumeration,
                    allowedValues = listOf("positive", "negative", "neutral")
                )
            ),
            template = "Classify as {{sentiment}}"
        )

        assertEquals(PromptArgType.enumeration, prompt.args[0].type)
        assertEquals(listOf("positive", "negative", "neutral"), prompt.args[0].allowedValues)
    }
}
