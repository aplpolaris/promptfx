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
package tri.ai.tool

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import tri.ai.pips.core.ExecContext
import tri.ai.pips.core.MAPPER

class JsonToolExecutableTest {

    companion object {
        val SAMPLE_EXECUTABLE1 = object : JsonToolExecutable(
            "calc", "Use this to do math",
            """{"type":"object","properties":{"input":{"type":"string"}}}"""
        ) {
            override suspend fun run(input: JsonObject, context: ExecContext) = "42"
        }
        val SAMPLE_EXECUTABLE2 = object : JsonToolExecutable(
            "romanize", "Converts numbers to Roman numerals",
            """{"type":"object","properties":{"input":{"type":"integer"}}}"""
        ) {
            override suspend fun run(input: JsonObject, context: ExecContext): String {
                val value = input["input"]?.jsonPrimitive?.int ?: throw RuntimeException("No input")
                return when (value) {
                    5 -> "V"
                    42 -> "XLII"
                    84 -> "LXXXIV"
                    else -> "I don't know"
                }
            }
        }
        val SAMPLE_EXECUTABLE4 = object : JsonToolExecutable(
            "other", "Answer a question that cannot be answered by the other tools",
            """{"type":"object","properties":{"input":{"type":"string"}}}"""
        ) {
            override suspend fun run(input: JsonObject, context: ExecContext) = "I don't know"
        }
        val SAMPLE_EXECUTABLES = listOf(SAMPLE_EXECUTABLE1, SAMPLE_EXECUTABLE2, SAMPLE_EXECUTABLE4)
    }

    private val testJsonTool = object : JsonToolExecutable("Calculator", "Does math calculations",
        """{"type":"object","properties":{"input":{"type":"string"}}}""") {
        override suspend fun run(input: JsonObject, context: ExecContext): String {
            val inputText = input["input"]?.toString()?.removeSurrounding("\"") ?: ""
            return when {
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
            val inputJson = MAPPER.createObjectNode().put("input", "2+2")
            val result = testJsonTool.execute(inputJson, context)
            assertEquals("4", result.get("result").asText())
        }
    }

    @Test
    fun testJsonToolExecutableProperties() {
        assertEquals("Calculator", testJsonTool.name)
        assertEquals("Does math calculations", testJsonTool.description)
        assertEquals("1.0.0", testJsonTool.version)
        assertNotNull(testJsonTool.inputSchema)
        assertNotNull(testJsonTool.outputSchema)
    }

    @Test
    fun testJsonToolExecutableSchemas() {
        val inputSchema = testJsonTool.inputSchema!!
        assertEquals("object", inputSchema.get("type").asText())
        assertNotNull(inputSchema.get("properties"))

        val outputSchema = testJsonTool.outputSchema!!
        assertEquals("object", outputSchema.get("type").asText())
        assertNotNull(outputSchema.get("properties").get("result"))
    }

    @Test
    fun testTools() {
        runTest {
            assertEquals(42, SAMPLE_EXECUTABLE1.run(
                buildJsonObject { put("input", "2+3") },
                ExecContext()
            ).toInt())
        }

        runTest {
            assertEquals("XLII", SAMPLE_EXECUTABLE2.run(
                buildJsonObject { put("input", 42) },
                ExecContext()
            ))
        }

        runTest {
            assertEquals("I don't know", SAMPLE_EXECUTABLE4.run(
                buildJsonObject { put("input", 100) },
                ExecContext()
            ))
        }
    }
}
