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
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tri.ai.pips.core.ExecContext
import tri.ai.pips.core.JsonToolExecutable

class JsonToolTest {

    companion object {
        val SAMPLE_TOOL1 = tool("calc", "Use this to do math",
            """{"type":"object","properties":{"input":{"type":"string"}}}""") {
            "42"
        }
        val SAMPLE_TOOL2 = tool("romanize", "Converts numbers to Roman numerals",
            """{"type":"object","properties":{"input":{"type":"integer"}}}""") {
            val value = it["input"]?.jsonPrimitive?.int ?: throw RuntimeException("No input")
            when (value) {
                5 -> "V"
                42 -> "XLII"
                84 -> "LXXXIV"
                else -> "I don't know"
            }
        }
        val SAMPLE_TOOL3 = tool("user", "Prompt the user for additional information you need",
            """{"type":"object","properties":{"user_request":{"type":"string"}}}""") {
            // get user input from console
            val input = it["user_request"]?.jsonPrimitive?.content ?: it.toString()
            print("I need some more information: $input >> ")
            readlnOrNull() ?: ""
        }
        val SAMPLE_TOOL4 = tool("other", "Answer a question that cannot be answered by the other tools",
            """{"type":"object","properties":{"input":{"type":"string"}}}""") {
            "I don't know"
        }
        val SAMPLE_TOOLS = listOf(SAMPLE_TOOL1, SAMPLE_TOOL2, SAMPLE_TOOL4)

        // Executable versions for the new system
        val SAMPLE_EXECUTABLE1 = object : JsonToolExecutable("calc", "Use this to do math",
            """{"type":"object","properties":{"input":{"type":"string"}}}""") {
            override suspend fun run(input: JsonObject, context: ExecContext) = "42"
        }
        val SAMPLE_EXECUTABLE2 = object : JsonToolExecutable("romanize", "Converts numbers to Roman numerals",
            """{"type":"object","properties":{"input":{"type":"integer"}}}""") {
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
        val SAMPLE_EXECUTABLE4 = object : JsonToolExecutable("other", "Answer a question that cannot be answered by the other tools",
            """{"type":"object","properties":{"input":{"type":"string"}}}""") {
            override suspend fun run(input: JsonObject, context: ExecContext) = "I don't know"
        }
        val SAMPLE_EXECUTABLES = listOf(SAMPLE_EXECUTABLE1, SAMPLE_EXECUTABLE2, SAMPLE_EXECUTABLE4)

        private fun tool(name: String, description: String, schema: String, op: (JsonObject) -> String) = object : JsonTool(name, description, schema) {
            override suspend fun run(input: JsonObject) = op(input)
        }
    }

    @Test
    fun testTools() {
        runTest {
            assertEquals(42, SAMPLE_TOOL1.run(
                buildJsonObject { put("input", "2+3") }
            ).toInt())
        }

        runTest {
            assertEquals("XLII", SAMPLE_TOOL2.run(
                buildJsonObject { put("input", 42) }
            ))
        }

        runTest {
            assertEquals("I don't know", SAMPLE_TOOL4.run(
                buildJsonObject { put("input", 100) }
            ))
        }
    }

}
