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
package tri.ai.prompt.server

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tri.ai.core.MChatMessagePart
import tri.ai.core.MPartType
import tri.ai.prompt.PromptLibrary

class McpPromptServerTest {

    val SERVER = McpPromptServer().apply {
        library = PromptLibrary.INSTANCE
    }

    @Test
    fun testListPrompts() {
        val prompts = SERVER.listPrompts()
        println(prompts)
        assertTrue(prompts.isNotEmpty())
    }

    @Test
    fun testGetPrompt() {
        val response = SERVER.getPrompt("text-qa/answer", mapOf(
            "instruct" to "What is the meaning of life?",
            "input" to "42"
        ))
        println(response)
        assertEquals(null, response.description)
        assertEquals(1, response.messages.size)
        assertEquals(1, response.messages[0].content!!.size)
        assertEquals(listOf(MChatMessagePart(
            partType = MPartType.TEXT,
            text = """
                Answer the following question about the text below. If you do not know the answer, say you don't know.
                ```
                42
                ```
                Question: What is the meaning of life?
                Answer:
                
            """.trimIndent()
        )), response.messages[0].content)
    }

}
