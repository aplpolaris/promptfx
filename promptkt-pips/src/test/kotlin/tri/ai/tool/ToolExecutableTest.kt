package tri.ai.tool

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tri.ai.pips.core.ExecContext
import tri.ai.pips.core.MAPPER

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
            val inputJson = MAPPER.createObjectNode().put("input", "2+2")
            val result = testTool.execute(inputJson, context)
            assertEquals("4", result.get("result").asText())
            assertEquals(false, result.get("isTerminal").asBoolean())
        }
    }

    @Test
    fun testToolExecutableWithRequestField() {
        runTest {
            val context = ExecContext()
            val inputJson = MAPPER.createObjectNode().put("request", "Can you multiply 21 times 2?")
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
                    ToolExecutableResult("done", isTerminal = true, finalResult = "Final answer")
            }

            val context = ExecContext()
            val inputJson = MAPPER.createObjectNode().put("input", "test")
            val result = terminalTool.execute(inputJson, context)

            assertEquals("done", result.get("result").asText())
            assertEquals(true, result.get("isTerminal").asBoolean())
            assertEquals("Final answer", result.get("finalResult").asText())
        }
    }
}