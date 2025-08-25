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
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import tri.ai.pips.core.ExecContext
import tri.ai.pips.core.MAPPER
import tri.ai.tool.JsonTool

class JsonToolExecutableTest {

    private val testJsonTool = object : JsonTool("Calculator", "Does math calculations",
        """{"type":"object","properties":{"input":{"type":"string"}}}""") {
        override suspend fun run(input: JsonObject): String {
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
        val executable = JsonToolExecutable(testJsonTool)
        val context = ExecContext()
        
        // Test with input object
        val inputJson = MAPPER.createObjectNode().put("input", "2+2")
        val result = executable.execute(inputJson, context)
        
        assertEquals("4", result.get("result").asText())
    }

    @Test
    fun testJsonToolExecutableWithComplexInput() = runTest {
        val executable = JsonToolExecutable(testJsonTool)
        val context = ExecContext()
        
        // Test with multiply request
        val inputJson = MAPPER.createObjectNode().put("input", "multiply 21 by 2")
        val result = executable.execute(inputJson, context)
        
        assertEquals("42", result.get("result").asText())
    }

    @Test
    fun testJsonToolExecutableProperties() {
        val executable = JsonToolExecutable(testJsonTool)
        
        assertEquals("Calculator", executable.name)
        assertEquals("Does math calculations", executable.description)
        assertEquals("1.0.0", executable.version)
        assertNotNull(executable.inputSchema)
        assertNotNull(executable.outputSchema)
    }

    @Test
    fun testJsonToolExecutableSchemas() {
        val executable = JsonToolExecutable(testJsonTool)
        
        // Check that input schema is parsed correctly
        val inputSchema = executable.inputSchema!!
        assertEquals("object", inputSchema.get("type").asText())
        assertNotNull(inputSchema.get("properties"))
        
        // Check that output schema exists
        val outputSchema = executable.outputSchema!!
        assertEquals("object", outputSchema.get("type").asText())
        assertNotNull(outputSchema.get("properties").get("result"))
    }

    @Test
    fun testJsonToolExecutableWrapperFactory() {
        val executable = JsonToolExecutable.wrap(testJsonTool)
        
        assertEquals("Calculator", executable.name)
        assertEquals("1.0.0", executable.version)
    }
}