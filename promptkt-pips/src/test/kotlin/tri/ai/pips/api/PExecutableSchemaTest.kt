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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.core.MChatVariation
import tri.ai.core.TextCompletion
import tri.ai.prompt.PromptArgDef
import tri.ai.prompt.PromptArgType
import tri.ai.prompt.PromptDef
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.tool.wf.MAPPER

class PExecutableSchemaTest {

    @Test
    fun `PPromptExecutable has input and output schemas`() {
        val prompt = PromptDef(
            id = "test/schema-prompt",
            args = listOf(PromptArgDef("input", "Input text", true, PromptArgType.string)),
            template = "Process: {{input}}"
        )
        
        val executable = PPromptExecutable(prompt)
        
        assertNotNull(executable.inputSchema)
        assertNotNull(executable.outputSchema)
        
        // Verify input schema has the expected structure
        val inputSchema = executable.inputSchema!!
        assertEquals("object", inputSchema.get("type").asText())
        assertTrue(inputSchema.get("properties").has("input"))
        
        // Verify output schema has the expected structure
        val outputSchema = executable.outputSchema!!
        assertEquals("object", outputSchema.get("type").asText())
        assertTrue(outputSchema.get("properties").has("text"))
    }

    @Test
    fun `PTextCompletionExecutable has proper schemas and execution`() {
        val mockCompletion = object : TextCompletion {
            override val modelId = "mock-completion"
            override suspend fun complete(
                text: String,
                variation: MChatVariation,
                tokens: Int?,
                stop: List<String>?,
                numResponses: Int?
            ): AiPromptTrace<String> {
                return AiPromptTrace.output("Completed: $text")
            }
        }
        
        val executable = PTextCompletionExecutable(mockCompletion)
        
        assertEquals("completion/mock-completion", executable.name)
        assertNotNull(executable.inputSchema)
        assertNotNull(executable.outputSchema)
        
        runBlocking {
            // Test with string input
            val stringInput = MAPPER.valueToTree<JsonNode>("Hello world")
            val result1 = executable.execute(stringInput, PExecContext())
            assertEquals("Completed: Hello world", result1.get("text").asText())
            
            // Test with object input having "text" field
            val objectInput = MAPPER.createObjectNode().put("text", "Test message")
            val result2 = executable.execute(objectInput, PExecContext())
            assertEquals("Completed: Test message", result2.get("text").asText())
            
            // Test with object input having "message" field
            val messageInput = MAPPER.createObjectNode().put("message", "Another test")
            val result3 = executable.execute(messageInput, PExecContext())
            assertEquals("Completed: Another test", result3.get("text").asText())
        }
    }

    @Test
    fun `PChatExecutable has updated schemas`() {
        // Create a simple mock chat for testing
        val mockChat = object : tri.ai.core.TextChat {
            override val modelId = "mock-chat"
            override suspend fun chat(
                messages: List<tri.ai.core.TextChatMessage>,
                variation: MChatVariation,
                tokens: Int?,
                stop: List<String>?,
                numResponses: Int?,
                requestJson: Boolean?
            ): AiPromptTrace<tri.ai.core.TextChatMessage> {
                return AiPromptTrace.output(tri.ai.core.TextChatMessage.assistant("Mock response"))
            }
        }
        
        val executable = PChatExecutable(mockChat)
        
        assertEquals("chat/mock-chat", executable.name)
        assertNotNull(executable.inputSchema)
        assertNotNull(executable.outputSchema)
        
        // Verify schemas have the expected structure
        val inputSchema = executable.inputSchema!!
        assertTrue(inputSchema.has("anyOf"))
        
        val outputSchema = executable.outputSchema!!
        assertEquals("object", outputSchema.get("type").asText())
        assertTrue(outputSchema.get("properties").has("message"))
    }

    @Test
    fun `schema validation in PPlanValidator works`() {
        // Create a prompt with specific input requirements
        val prompt = PromptDef(
            id = "strict/prompt",
            args = listOf(
                PromptArgDef("name", "Person name", true, PromptArgType.string),
                PromptArgDef("age", "Person age", true, PromptArgType.integer)
            ),
            template = "Hello {{name}}, you are {{age}} years old."
        )
        
        val executable = PPromptExecutable(prompt)
        val registry = PExecutableRegistry.create(listOf(executable))
        
        // Valid plan should pass
        val validPlan = PPlan(
            id = "test-valid",
            steps = listOf(
                PPlanStep(
                    tool = "prompt/strict/prompt",
                    input = MAPPER.createObjectNode().put("name", "Alice").put("age", 25),
                    saveAs = "result"
                )
            )
        )
        
        // This should not throw
        assertDoesNotThrow {
            PPlanValidator.validateSchemas(validPlan, registry)
        }
        
        // Invalid plan should fail
        val invalidPlan = PPlan(
            id = "test-invalid",
            steps = listOf(
                PPlanStep(
                    tool = "prompt/strict/prompt",
                    input = MAPPER.createObjectNode().put("name", "Bob"), // missing required "age"
                    saveAs = "result"
                )
            )
        )
        
        // This should throw
        assertThrows(IllegalArgumentException::class.java) {
            PPlanValidator.validateSchemas(invalidPlan, registry)
        }
    }
}