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

import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tri.ai.prompt.server.McpPrompt
import tri.ai.prompt.server.McpPromptArg
import tri.ai.prompt.server.toMcpContract

class PromptDefTest {

    val TEST_PROMPT = PromptDef(
        id = "test-prompt-id",
        category = "category",
        name = "test-prompt",
        title = "Test Prompt",
        description = "This is a test prompt template.",
        args = listOf(PromptArgDef("color", "Provide a color", true)),
        template = "Turn the color {{color}} into a hex code."
    )

    @Test
    fun testPromptWrite() {
        val json = PromptGroupIO.MAPPER.writeValueAsString(TEST_PROMPT)
        println(json)
        Assertions.assertTrue(json.isNotEmpty())
    }

    @Test
    fun testPromptRead() {
        val json = PromptGroupIO.MAPPER.writeValueAsString(TEST_PROMPT)
        val prompt = PromptGroupIO.MAPPER.readValue<PromptDef>(json)
        assertEquals(TEST_PROMPT, prompt)
        assertEquals(json, PromptGroupIO.MAPPER.writeValueAsString(prompt))
    }

    @Test
    fun testPromptFill() {
        val result1 = TEST_PROMPT.fill("color" to "red")
        println(result1)
        assertEquals("Turn the color red into a hex code.", result1)
    }

    @Test
    fun testToMcpContract() {
        val mcp = TEST_PROMPT.toMcpContract()
        println(PromptGroupIO.MAPPER.writeValueAsString(mcp))
        val expected = McpPrompt(
            id = "test-prompt-id",
            name = "test-prompt",
            title = "Test Prompt",
            description = "This is a test prompt template.",
            arguments = listOf(McpPromptArg("color", "Provide a color", true))
        )
        assertEquals(expected, mcp)
    }

}

