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
import tri.ai.core.MChatVariation
import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.openai.OpenAiPlugin
import tri.ai.pips.core.ChatExecutable
import tri.ai.pips.core.ExecContext
import tri.ai.pips.core.Executable
import tri.ai.pips.core.ExecutableRegistry
import tri.ai.pips.core.PromptLibraryExecutableRegistry
import tri.ai.prompt.PromptLibrary
import tri.ai.prompt.trace.AiPromptTrace

class PPlanExecutorTest {

    @Test
    fun `simple plan executes with dummy tool`() {
        runBlocking {
            // --- Define a trivial Executable that just echoes its input ---
            val echo = object : Executable {
                override val name = "util/echo"
                override val description = "Echoes the input as output"
                override val version = "0.0.1"
                override val inputSchema: JsonNode? = null
                override val outputSchema: JsonNode? = null
                override suspend fun execute(input: JsonNode, ctx: ExecContext) = input
            }
            val registry = ExecutableRegistry.create(listOf(echo))

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
            val context = ExecContext()
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
            val registry = ExecutableRegistry.create(
                PromptLibraryExecutableRegistry(PromptLibrary.INSTANCE).list() +
                        ChatExecutable(OpenAiPlugin().chatModels().first())
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
            val context = ExecContext()
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


    @Test
    fun `multi-part prompt and LLM flow with variable chaining`() {
        runBlocking {
            // --- Create a mock chat service that returns predictable results ---
            val mockChat = object : TextChat {
                override val modelId = "mock-chat"
                override suspend fun chat(
                    messages: List<TextChatMessage>,
                    variation: MChatVariation,
                    tokens: Int?,
                    stop: List<String>?,
                    numResponses: Int?,
                    requestJson: Boolean?
                ): AiPromptTrace {
                    val message = messages.firstOrNull()?.content ?: "unknown"
                    val response = when {
                        "keywords" in message.lowercase() ->
                            "AI, machine learning, natural language processing"
                        "summarize" in message.lowercase() && "AI" in message ->
                            "This analysis covers key concepts in artificial intelligence."
                        else -> "Mock response"
                    }
                    return AiPromptTrace.outputMessage(TextChatMessage.assistant(response))
                }
            }

            val registry = ExecutableRegistry.create(
                PromptLibraryExecutableRegistry(PromptLibrary.INSTANCE).list() +
//                        PChatExecutable(OpenAiPlugin().chatModels().first())
                        ChatExecutable(mockChat)
            )

            // --- Multi-step plan JSON with variable chaining ---
            val json = """
                {
                  "id": "multi-step/analysis@0.1.0",
                  "steps": [
                    { 
                      "tool": "prompt/text-extract/keywords", 
                      "input": { "input": "Artificial intelligence and machine learning are transforming natural language processing capabilities." }, 
                      "saveAs": "keywordPrompt" 
                    },
                    { 
                      "tool": "chat/mock-chat", 
                      "input": { "${"$"}var": "keywordPrompt" }, 
                      "saveAs": "extractedKeywords" 
                    },
                    { 
                      "tool": "prompt/text-summarize/summarize", 
                      "input": { "input": { "${"$"}var": "extractedKeywords", "${"$"}ptr": "/message" }, "audience": "AI researchers", "style": "a survey paper abstract" }, 
                      "saveAs": "summaryPrompt" 
                    },
                    { 
                      "tool": "chat/mock-chat", 
                      "input": { "message": { "${"$"}var": "summaryPrompt" } }, 
                      "saveAs": "finalSummary" 
                    }
                  ]
                }
            """.trimIndent()

            val plan = PPlan.parse(json)
            val context = ExecContext()
            PPlanExecutor(registry).execute(plan, context)
            println("        Context: ${context.vars}")

            // --- Assertions ---
            assertEquals("multi-step/analysis@0.1.0", plan.id)
            assertEquals(4, plan.steps.size)

            // Verify all steps saved their results
            assertTrue("keywordPrompt" in context.vars.keys)
            assertTrue("extractedKeywords" in context.vars.keys)
            assertTrue("summaryPrompt" in context.vars.keys)
            assertTrue("finalSummary" in context.vars.keys)

            // Verify the flow worked - keyword prompt should contain the input text
            val keywordPrompt = context.vars["keywordPrompt"]?.get("text")?.asText() ?: ""
            assertTrue("Artificial intelligence" in keywordPrompt)

            // Verify the mock chat responses are flowing through
            val extractedKeywords = context.vars["extractedKeywords"]?.get("message")?.asText() ?: ""
            assertEquals("AI, machine learning, natural language processing", extractedKeywords)

            // Verify the final summary prompt contains the extracted keywords
            val summaryPrompt = context.vars["summaryPrompt"]?.get("text")?.asText() ?: ""
            assertTrue("AI, machine learning, natural language processing" in summaryPrompt)

            // Verify the final summary contains expected content
            val finalSummary = context.vars["finalSummary"]?.get("message")?.asText() ?: ""
            assertEquals("This analysis covers key concepts in artificial intelligence.", finalSummary)
        }
    }

}
