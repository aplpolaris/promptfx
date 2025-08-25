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

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tri.ai.pips.core.ExecContext
import tri.ai.pips.core.Executable
import tri.ai.pips.core.MAPPER
import com.fasterxml.jackson.databind.JsonNode

class AgentExecutableTest {

    // Simple calculator tool for testing
    private val calculatorExecutable = object : Executable {
        override val name = "Calculator"
        override val description = "Does basic math calculations"
        override val version = "1.0.0"
        override val inputSchema: JsonNode? = null
        override val outputSchema: JsonNode? = null
        
        override suspend fun execute(input: JsonNode, context: ExecContext): JsonNode {
            val inputText = input.get("input")?.asText() ?: ""
            val result = when {
                "2+2" in inputText || "2 + 2" in inputText -> "4"
                "21*2" in inputText || "21 * 2" in inputText -> "42"
                else -> "Calculation complete"
            }
            return MAPPER.createObjectNode().put("result", result)
        }
    }

    @Test
    fun testAgentExecutableProperties() {
        val agent = AgentExecutable(
            name = "TestAgent",
            description = "A test agent",
            version = "1.0.0",
            inputSchema = null,
            outputSchema = null,
            tools = listOf(calculatorExecutable)
        )
        
        assertEquals("TestAgent", agent.name)
        assertEquals("A test agent", agent.description)
        assertEquals("1.0.0", agent.version)
        assertEquals(1, agent.tools.size)
    }

    @Test
    fun testAgentExecutableContextValidation() = runTest {
        val agent = AgentExecutable(
            name = "TestAgent",
            description = "A test agent",
            version = "1.0.0",
            inputSchema = null,
            outputSchema = null,
            tools = listOf(calculatorExecutable)
        )
        
        // Test with empty context (should throw exception)
        val context = ExecContext()
        val input = MAPPER.createObjectNode().put("request", "What is 2+2?")
        
        try {
            agent.execute(input, context)
            org.junit.jupiter.api.Assertions.fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            org.junit.jupiter.api.Assertions.assertTrue(e.message!!.contains("TextCompletion"))
        }
    }
}