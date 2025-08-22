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
package tri.ai.core

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JsonRetryProcessorTest {

    @Test
    fun `should return first valid JSON result`() = runTest {
        var attemptCount = 0
        
        val result = JsonRetryProcessor.generateWithJsonRetry(
            config = JsonRetryConfig(maxAttempts = 3)
        ) { attempt ->
            attemptCount++
            when (attempt) {
                1 -> "This is just text without JSON"
                2 -> """{"success": true, "message": "Found it!"}"""
                3 -> "This won't be reached"
                else -> "Error"
            }
        }
        
        assertEquals(2, attemptCount)
        assertTrue(result.hasValidJson)
        assertNotNull(result.jsonElement)
        assertTrue(result.jsonElement is JsonObject)
        assertEquals(2, result.attemptNumber)
    }

    @Test
    fun `should return last result when no valid JSON found`() = runTest {
        val result = JsonRetryProcessor.generateWithJsonRetry(
            config = JsonRetryConfig(maxAttempts = 2)
        ) { attempt ->
            "Attempt $attempt: just plain text"
        }
        
        assertTrue(!result.hasValidJson)
        assertEquals(2, result.attemptNumber)
        assertEquals("Attempt 2: just plain text", result.responseText)
    }

    @Test
    fun `should respect JSON type requirements`() = runTest {
        val result = JsonRetryProcessor.generateWithJsonRetry(
            config = JsonRetryConfig(
                maxAttempts = 3,
                requireType = JsonRetryConfig.JsonType.ARRAY
            )
        ) { attempt ->
            when (attempt) {
                1 -> """{"this": "is an object"}"""
                2 -> """[1, 2, 3, "this is an array"]"""
                3 -> "Won't be reached"
                else -> "Error"
            }
        }
        
        assertTrue(result.hasValidJson)
        assertTrue(result.jsonElement is kotlinx.serialization.json.JsonArray)
        assertEquals(2, result.attemptNumber)
    }

    @Test
    fun `should require complete response to be JSON when configured`() = runTest {
        val result = JsonRetryProcessor.generateWithJsonRetry(
            config = JsonRetryConfig(
                maxAttempts = 3,
                requireCompleteResponse = true
            )
        ) { attempt ->
            when (attempt) {
                1 -> "Here's your JSON: {\"data\": \"value\"}"
                2 -> """{"complete": "json", "response": true}"""
                3 -> "Won't be reached"
                else -> "Error"
            }
        }
        
        assertTrue(result.hasValidJson)
        assertEquals(2, result.attemptNumber)
        assertTrue(result.isValid(JsonRetryConfig(requireCompleteResponse = true)))
    }

    @Test
    fun `should generate multiple results with JSON analysis`() = runTest {
        val results = JsonRetryProcessor.generateMultipleWithJson(3) { attempt ->
            when (attempt) {
                1 -> "No JSON here"
                2 -> """{"attempt": 2, "hasJson": true}"""
                3 -> "[1, 2, 3]"
                else -> "Error"
            }
        }
        
        assertEquals(3, results.size)
        assertTrue(!results[0].hasValidJson)
        assertTrue(results[1].hasValidJson)
        assertTrue(results[2].hasValidJson)
        assertTrue(results[1].jsonElement is JsonObject)
        assertTrue(results[2].jsonElement is kotlinx.serialization.json.JsonArray)
    }

    @Test
    fun `should find best result from multiple attempts`() {
        val results = listOf(
            JsonGenerationResult("No JSON", null, false, 1, null),
            JsonGenerationResult("Has {\"json\": true}", 
                kotlinx.serialization.json.Json.parseToJsonElement("{\"json\": true}"), 
                true, 2, "{\"json\": true}"),
            JsonGenerationResult("Also has [1,2,3]", 
                kotlinx.serialization.json.Json.parseToJsonElement("[1,2,3]"), 
                true, 3, "[1,2,3]")
        )
        
        val best = JsonRetryProcessor.getBestResult(results)
        
        assertNotNull(best)
        assertTrue(best!!.hasValidJson)
        assertEquals(2, best.attemptNumber)
    }

    @Test
    fun `should handle generator exceptions gracefully`() = runTest {
        val result = JsonRetryProcessor.generateWithJsonRetry(
            config = JsonRetryConfig(maxAttempts = 2)
        ) { attempt ->
            if (attempt == 1) {
                throw RuntimeException("Simulated failure")
            } else {
                """{"recovered": true}"""
            }
        }
        
        assertTrue(result.hasValidJson)
        assertEquals(2, result.attemptNumber)
    }

    @Test
    fun `should validate result correctly`() {
        val objectResult = JsonGenerationResult(
            """{"test": "object"}""",
            kotlinx.serialization.json.Json.parseToJsonElement("""{"test": "object"}"""),
            true,
            1,
            """{"test": "object"}"""
        )
        
        val arrayResult = JsonGenerationResult(
            """[1, 2, 3]""",
            kotlinx.serialization.json.Json.parseToJsonElement("""[1, 2, 3]"""),
            true,
            1,
            """[1, 2, 3]"""
        )
        
        // Test default config
        assertTrue(objectResult.isValid(JsonRetryConfig()))
        assertTrue(arrayResult.isValid(JsonRetryConfig()))
        
        // Test object requirement
        assertTrue(objectResult.isValid(JsonRetryConfig(requireType = JsonRetryConfig.JsonType.OBJECT)))
        assertTrue(!arrayResult.isValid(JsonRetryConfig(requireType = JsonRetryConfig.JsonType.OBJECT)))
        
        // Test array requirement
        assertTrue(!objectResult.isValid(JsonRetryConfig(requireType = JsonRetryConfig.JsonType.ARRAY)))
        assertTrue(arrayResult.isValid(JsonRetryConfig(requireType = JsonRetryConfig.JsonType.ARRAY)))
    }
}