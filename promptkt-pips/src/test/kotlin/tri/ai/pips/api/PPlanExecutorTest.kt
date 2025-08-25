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
package tri.ai.pips.api

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import tri.ai.openai.OpenAiPlugin
import tri.ai.prompt.PromptLibrary

class PPlanExecutorTest {

    @Test
    fun `simple plan executes with dummy tool`() {
        runBlocking {
            // --- Define a trivial Executable that just echoes its input ---
            val echo = object : PExecutable {
                override val name = "util/echo"
                override val version = "0.0.1"
                override val inputSchema: JsonNode? = null
                override val outputSchema: JsonNode? = null
                override suspend fun execute(input: JsonNode, ctx: PExecContext) = input
            }
            val registry = PExecutableRegistry.create(listOf(echo))

            // --- Minimal plan JSON ---
            val json = """
                {
                  "id": "smoke/demo@0.0.1",
                  "steps": [
                    { "tool": "util/echo", "input": { "msg": "hi" }, "saveAs": "out" }
                  ]
                }
            """.trimIndent()

            val plan = PPlan.parse(json)
            val context = PExecContext()
            PPlanExecutor(registry).execute(plan, context)
            println("        Context: ${context.vars}")

            // --- Assertions ---
            assertEquals("smoke/demo@0.0.1", plan.id)
            assertTrue("out" in context.vars.keys)
            assertEquals("hi", (context.vars["out"] as JsonNode).get("msg").asText())
        }
    }

    @Test
    fun `plan using a PromptLibrary and LLM`() {
        runBlocking {
            val registry = PExecutableRegistry.create(
                PPromptLibraryExecutableRegistry(PromptLibrary.INSTANCE).list() +
                        PChatExecutable(OpenAiPlugin().chatModels().first())
            )

            // --- Minimal plan JSON ---
            val json = """
                {
                  "id": "prompt-llm/demo@0.0.1",
                  "steps": [
                    { "tool": "prompt/examples/color", "input": { "input": "red" }, "saveAs": "prompt1" },
                    { "tool": "chat/gpt-3.5-turbo", "input": { "message": { "${"$"}var": "prompt1" } }, "saveAs": "chat1" }
                  ]
                }
            """.trimIndent()

            val plan = PPlan.parse(json)
            val context = PExecContext()
            PPlanExecutor(registry).execute(plan, context)
            println("        Context: ${context.vars}")
            println("        Chat response: ${context.vars["chat1"]?.get("message")?.asText()}")

            // --- Assertions ---
            assertEquals("prompt-llm/demo@0.0.1", plan.id)
            assertTrue("prompt1" in context.vars.keys)
            assertTrue("chat1" in context.vars.keys)
            assertTrue("red" in (context.vars["prompt1"]?.get("text")?.asText() ?: ""))
            assertEquals("#ff0000", context.vars["chat1"]?.get("message")?.asText()?.toLowerCase())
        }
    }

}
