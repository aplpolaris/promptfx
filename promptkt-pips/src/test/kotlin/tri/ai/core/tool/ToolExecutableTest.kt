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
package tri.ai.core.tool

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tri.util.json.createObject

class ToolExecutableTest {

    private val testTool = object : ToolExecutable("Calculator", "Does basic math") {
        override suspend fun run(input: String, context: ExecContext): ToolExecutableResult {
            return when {
                "2+2" in input -> ToolExecutableResult("4")
                "multiply" in input -> ToolExecutableResult("42")
                else -> ToolExecutableResult("Unknown calculation")
            }
        }
    }

    @Test
    fun testToolExecutableBasicExecution() {
        runTest {
            val context = ExecContext()
            val inputJson = createObject("input", "2+2")
            val result = testTool.execute(inputJson, context)
            assertEquals("4", result.get("result").asText())
            assertEquals(false, result.get("isTerminal").asBoolean())
        }
    }

    @Test
    fun testToolExecutableWithRequestField() {
        runTest {
            val context = ExecContext()
            val inputJson = createObject("request", "Can you multiply 21 times 2?")
            val result = testTool.execute(inputJson, context)
            assertEquals("42", result.get("result").asText())
        }
    }

    @Test
    fun testToolExecutableProperties() {
        assertEquals("Calculator", testTool.name)
        assertEquals("Does basic math", testTool.description)
        assertEquals("1.0.0", testTool.version)
    }

    @Test
    fun testToolExecutableWithTerminalResult() {
        runTest {
            val terminalTool = object : ToolExecutable("Terminal", "Terminal tool") {
                override suspend fun run(input: String, context: ExecContext) =
                    ToolExecutableResult("Final answer", isTerminal = true)
            }

            val context = ExecContext()
            val inputJson = createObject("input", "test")
            val result = terminalTool.execute(inputJson, context)

            assertEquals("Final answer", result.get("result").asText())
            assertEquals(true, result.get("isTerminal").asBoolean())
        }
    }
}
