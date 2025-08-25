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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tri.ai.pips.core.ExecContext
import tri.ai.pips.core.MAPPER
import tri.ai.tool.Tool
import tri.ai.tool.ToolDict
import tri.ai.tool.ToolResult

class ToolExecutableTest {

    private val testTool = object : Tool("Calculator", "Does basic math") {
        override suspend fun run(input: ToolDict): ToolResult {
            val inputText = input["input"] ?: ""
            return when {
                "2+2" in inputText -> ToolResult("4")
                "multiply" in inputText -> ToolResult("42")
                else -> ToolResult("Unknown calculation")
            }
        }
    }

    @Test
    fun testToolExecutableBasicExecution() = runTest {
        val executable = ToolExecutable(testTool)
        val context = ExecContext()
        
        // Test with string input
        val inputJson = MAPPER.createObjectNode().put("input", "2+2")
        val result = executable.execute(inputJson, context)
        
        assertEquals("4", result.get("result").asText())
        assertEquals(false, result.get("isTerminal").asBoolean())
    }

    @Test
    fun testToolExecutableWithRequestField() = runTest {
        val executable = ToolExecutable(testTool)
        val context = ExecContext()
        
        // Test with request field
        val inputJson = MAPPER.createObjectNode().put("request", "Can you multiply 21 times 2?")
        val result = executable.execute(inputJson, context)
        
        assertEquals("42", result.get("result").asText())
    }

    @Test
    fun testToolExecutableProperties() {
        val executable = ToolExecutable(testTool)
        
        assertEquals("Calculator", executable.name)
        assertEquals("Does basic math", executable.description)
        assertEquals("1.0.0", executable.version)
    }

    @Test
    fun testToolExecutableWrapperFactory() {
        val executable = ToolExecutable.wrap(testTool)
        
        assertTrue(executable is ToolExecutable)
        assertEquals("Calculator", executable.name)
    }
}