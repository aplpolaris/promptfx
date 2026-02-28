/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import tri.util.json.INTEGER_INPUT_SCHEMA
import tri.util.json.STRING_INPUT_SCHEMA
import tri.util.json.createObject
import tri.util.json.inputText

class JsonToolExecutableTest {

    companion object {
        private fun stringInputTool(name: String, description: String, op: (String) -> String) = object : JsonToolExecutable(name, description, STRING_INPUT_SCHEMA) {
            override suspend fun run(input: JsonNode, context: ExecContext): String {
                val inputText = input.inputText
                return op(inputText)
            }
        }
        private fun intInputTool(name: String, description: String, op: (Int) -> String) = object : JsonToolExecutable(name, description, INTEGER_INPUT_SCHEMA) {
            override suspend fun run(input: JsonNode, context: ExecContext): String {
                val value = input["input"]?.asInt() ?: throw RuntimeException("No input")
                return op(value)
            }
        }

        val SAMPLE_EXECUTABLE1 = stringInputTool("calc", "Use this to do math") {
            "42"
        }
        val SAMPLE_EXECUTABLE2 = intInputTool("romanize", "Converts numbers to Roman numerals") {
            when (it) {
                5 -> "V"
                42 -> "XLII"
                84 -> "LXXXIV"
                else -> "I don't know"
            }
        }
        val SAMPLE_EXECUTABLE4 = stringInputTool("other", "Answer a question that cannot be answered by the other tools") {
            "I don't know"
        }
        val SAMPLE_EXECUTABLES = listOf(SAMPLE_EXECUTABLE1, SAMPLE_EXECUTABLE2, SAMPLE_EXECUTABLE4)

        private val TEST_JSON_TOOL = stringInputTool("Calculator", "Does math calculations") {
            val inputText = it.trim().removeSurrounding("\"")
            when {
                "2+2" in inputText -> "4"
                "multiply" in inputText -> "42"
                else -> "Unknown calculation"
            }
        }
    }

    @Test
    fun testJsonToolExecutableBasicExecution() {
        runTest {
            val context = ExecContext()
            val inputJson = createObject("input", "2+2")
            val result = TEST_JSON_TOOL.execute(inputJson, context)
            assertEquals("4", result.get("result").asText())
        }
    }

    @Test
    fun testJsonToolExecutableProperties() {
        assertEquals("Calculator", TEST_JSON_TOOL.name)
        assertEquals("Does math calculations", TEST_JSON_TOOL.description)
        assertEquals("1.0.0", TEST_JSON_TOOL.version)
        assertNotNull(TEST_JSON_TOOL.inputSchema)
        assertNotNull(TEST_JSON_TOOL.outputSchema)
    }

    @Test
    fun testJsonToolExecutableSchemas() {
        val inputSchema = TEST_JSON_TOOL.inputSchema
        assertEquals("object", inputSchema.get("type").asText())
        assertNotNull(inputSchema.get("properties"))

        val outputSchema = TEST_JSON_TOOL.outputSchema
        assertEquals("object", outputSchema.get("type").asText())
        assertNotNull(outputSchema.get("properties").get("result"))
    }

    @Test
    fun testTools() {
        runTest {
            assertEquals(42, SAMPLE_EXECUTABLE1.run(createObject("input", "2+3"), ExecContext()).toInt())
        }

        runTest {
            assertEquals("XLII", SAMPLE_EXECUTABLE2.run(createObject("input", 42), ExecContext()))
        }

        runTest {
            assertEquals("I don't know", SAMPLE_EXECUTABLE4.run(createObject("input", 100), ExecContext()))
        }
    }
}
