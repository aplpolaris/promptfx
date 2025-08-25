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

import org.junit.jupiter.api.Test
import tri.ai.pips.core.MAPPER
import tri.ai.tool.JsonTool
import tri.ai.tool.Tool
import tri.ai.tool.ToolDict
import tri.ai.tool.ToolResult
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * Integration test showing how legacy Tool and JsonTool objects can be 
 * converted to the new Executable interface and used in a modern workflow.
 */
class ExecutableMigrationIntegrationTest {

    // Legacy Tool object (as used in existing codebase)
    private val calculatorTool = object : Tool("Calculator", "Use this to do math") {
        override suspend fun run(input: ToolDict): ToolResult {
            val inputText = input["input"] ?: ""
            return when {
                "2+2" in inputText -> ToolResult("4")
                "42" in inputText -> ToolResult("42")
                "multiply 21 times 2" in inputText -> ToolResult("42")
                else -> ToolResult("Unknown calculation")
            }
        }
    }

    // Legacy JsonTool object (as used in existing codebase)
    private val romanizerTool = object : JsonTool("Romanizer", "Converts numbers to Roman numerals",
        """{"type":"object","properties":{"input":{"type":"integer"}}}""") {
        override suspend fun run(input: JsonObject): String {
            val value = input["input"]?.toString()?.toIntOrNull() ?: 0
            return when (value) {
                4 -> "IV"
                42 -> "XLII"
                84 -> "LXXXIV"
                else -> "Unknown number"
            }
        }
    }

    @Test
    fun testToolToExecutableMigration() {
        // Legacy way: Tool objects used directly
        val legacyTool = calculatorTool
        assertEquals("Calculator", legacyTool.name)
        assertEquals("Use this to do math", legacyTool.description)
        
        // New way: Tool wrapped as Executable
        val modernExecutable = ToolExecutable.wrap(calculatorTool)
        assertEquals("Calculator", modernExecutable.name)
        assertEquals("Use this to do math", modernExecutable.description)
        assertEquals("1.0.0", modernExecutable.version) // Added versioning
        
        println("✓ Successfully converted Tool to Executable")
    }

    @Test
    fun testJsonToolToExecutableMigration() {
        // Legacy way: JsonTool objects used directly
        val legacyJsonTool = romanizerTool
        assertEquals("Romanizer", legacyJsonTool.tool.name)
        
        // New way: JsonTool wrapped as Executable
        val modernExecutable = JsonToolExecutable.wrap(romanizerTool)
        assertEquals("Romanizer", modernExecutable.name)
        assertEquals("Converts numbers to Roman numerals", modernExecutable.description)
        assertNotNull(modernExecutable.inputSchema)
        assertNotNull(modernExecutable.outputSchema)
        
        println("✓ Successfully converted JsonTool to Executable")
    }

    @Test
    fun testToolChainToAgentMigration() {
        // Legacy way: Tools used with ToolChainExecutor
        val legacyTools = listOf(calculatorTool)
        assertEquals(1, legacyTools.size)
        
        // New way: Convert Tools to Executables and use with AgentExecutable
        val modernExecutables = legacyTools.map { ToolExecutable.wrap(it) }
        
        val agent = AgentExecutable(
            name = "MathAgent",
            description = "An agent that can perform math calculations",
            version = "1.0.0",
            inputSchema = null,
            outputSchema = null,
            tools = modernExecutables
        )
        
        assertEquals("MathAgent", agent.name)
        assertEquals(1, agent.tools.size)
        assertEquals("Calculator", agent.tools[0].name)
        
        println("✓ Successfully created agent from legacy tools")
    }

    @Test
    fun testMixedToolTypesToAgent() {
        // Show that both Tool and JsonTool can be used together in an agent
        val mixedExecutables = listOf(
            ToolExecutable.wrap(calculatorTool),      // From legacy Tool
            JsonToolExecutable.wrap(romanizerTool)    // From legacy JsonTool
        )
        
        val multiToolAgent = AgentExecutable(
            name = "MathAndRomanAgent",
            description = "An agent that can do math and convert to Roman numerals",
            version = "1.0.0",
            inputSchema = null,
            outputSchema = null,
            tools = mixedExecutables
        )
        
        assertEquals(2, multiToolAgent.tools.size)
        assertEquals("Calculator", multiToolAgent.tools[0].name)
        assertEquals("Romanizer", multiToolAgent.tools[1].name)
        
        println("✓ Successfully created multi-tool agent from mixed legacy tools")
    }

    @Test
    fun testSchemaValidation() {
        // Test that schemas are properly preserved in the migration
        val jsonToolExecutable = JsonToolExecutable.wrap(romanizerTool)
        
        val inputSchema = jsonToolExecutable.inputSchema!!
        assertEquals("object", inputSchema.get("type").asText())
        assertNotNull(inputSchema.get("properties").get("input"))
        
        val outputSchema = jsonToolExecutable.outputSchema!!
        assertEquals("object", outputSchema.get("type").asText())
        assertNotNull(outputSchema.get("properties").get("result"))
        
        println("✓ JSON schemas properly preserved during migration")
    }
}