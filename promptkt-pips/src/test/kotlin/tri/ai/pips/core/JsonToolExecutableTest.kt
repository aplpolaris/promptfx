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
package tri.ai.pips.core

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class JsonToolExecutableTest {

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
    fun testJsonToolExecutableBasicExecution() = runTest {
        val context = ExecContext()
        
        // Test with input object
        val inputJson = MAPPER.createObjectNode().put("input", "2+2")
        val result = testJsonTool.execute(inputJson, context)
        
        assertEquals("4", result.get("result").asText())
    }

    @Test
    fun testJsonToolExecutableWithComplexInput() = runTest {
        val context = ExecContext()
        
        // Test with multiply request
        val inputJson = MAPPER.createObjectNode().put("input", "multiply 21 by 2")
        val result = testJsonTool.execute(inputJson, context)
        
        assertEquals("42", result.get("result").asText())
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
        // Check that input schema is parsed correctly
        val inputSchema = testJsonTool.inputSchema!!
        assertEquals("object", inputSchema.get("type").asText())
        assertNotNull(inputSchema.get("properties"))
        
        // Check that output schema exists
        val outputSchema = testJsonTool.outputSchema!!
        assertEquals("object", outputSchema.get("type").asText())
        assertNotNull(outputSchema.get("properties").get("result"))
    }
}