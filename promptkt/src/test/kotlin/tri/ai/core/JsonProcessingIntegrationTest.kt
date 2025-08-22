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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Integration test demonstrating all JSON processing features implemented
 * for issues #188, #189, #190, and #100.
 */
class JsonProcessingIntegrationTest {

    @Test
    fun `demonstrates issue 189 - extract first valid JSON from various response formats`() {
        // Test with JSON embedded in text
        val textWithJson = """
            Here's the data you requested:
            {"name": "John Doe", "age": 30, "occupation": "Developer"}
            Hope this helps!
        """.trimIndent()
        
        val extracted = JsonResponseProcessor.extractFirstValidJson(textWithJson)
        assertNotNull(extracted, "Should extract JSON from embedded text")
        assertTrue(extracted is kotlinx.serialization.json.JsonObject)
        
        // Test with JSON in code block
        val codeBlockText = """
            The result is:
            ```json
            [{"id": 1, "name": "Item 1"}, {"id": 2, "name": "Item 2"}]
            ```
        """.trimIndent()
        
        val codeBlockJson = JsonResponseProcessor.extractFirstValidJson(codeBlockText)
        assertNotNull(codeBlockJson, "Should extract JSON from code block")
        assertTrue(codeBlockJson is kotlinx.serialization.json.JsonArray)
        
        // Test extracting raw text for copying
        val rawJson = JsonResponseProcessor.extractFirstJsonText(textWithJson)
        assertEquals("""{"name": "John Doe", "age": 30, "occupation": "Developer"}""", rawJson)
    }

    @Test 
    fun `demonstrates issue 190 - multi-attempt JSON generation with retry logic`() = runTest {
        var attempt = 0
        
        // Simulate a generator that fails first few times, then succeeds
        val result = JsonRetryProcessor.generateWithJsonRetry(
            config = JsonRetryConfig(maxAttempts = 4, requireType = JsonRetryConfig.JsonType.OBJECT)
        ) { attemptNumber ->
            attempt++
            when (attemptNumber) {
                1 -> "This is not JSON at all"
                2 -> "Still no JSON here"  
                3 -> "[1, 2, 3]"  // Valid JSON but wrong type (array, not object)
                4 -> """{"success": true, "message": "Finally got valid JSON object!"}"""
                else -> "Should not reach here"
            }
        }
        
        assertEquals(4, result.attemptNumber, "Should take 4 attempts to get valid object")
        assertTrue(result.hasValidJson, "Final result should have valid JSON")
        assertTrue(result.jsonElement is kotlinx.serialization.json.JsonObject, "Should be a JSON object")
        assertTrue(result.isValid(JsonRetryConfig(requireType = JsonRetryConfig.JsonType.OBJECT)))
    }

    @Test
    fun `demonstrates multiple attempts with analysis`() = runTest {
        val attempts = JsonRetryProcessor.generateMultipleWithJson(3) { attemptNumber ->
            when (attemptNumber) {
                1 -> "No JSON here"
                2 -> """{"attempt": 2, "hasJson": true}"""
                3 -> """[1, 2, 3, "attempt 3"]"""
                else -> "Error"
            }
        }
        
        assertEquals(3, attempts.size)
        assertFalse(attempts[0].hasValidJson, "First attempt should have no JSON")
        assertTrue(attempts[1].hasValidJson, "Second attempt should have JSON")  
        assertTrue(attempts[2].hasValidJson, "Third attempt should have JSON")
        
        val best = JsonRetryProcessor.getBestResult(attempts)
        assertEquals(2, best!!.attemptNumber, "Should select second attempt as best")
        assertTrue(best.jsonElement is kotlinx.serialization.json.JsonObject)
    }

    @Test
    fun `demonstrates JSON formatting for display`() {
        val compactJson = """{"name":"John","details":{"age":30,"city":"NYC"}}"""
        
        val formatted = JsonResponseProcessor.formatJson(compactJson)
        assertNotNull(formatted, "Should format JSON successfully")
        assertTrue(formatted!!.contains("  "), "Should have indentation")
        assertTrue(formatted.contains("\n"), "Should have newlines")
        
        // Verify it's still valid JSON after formatting
        val parsed = JsonResponseProcessor.extractFirstValidJson(formatted)
        assertNotNull(parsed, "Formatted JSON should still be parseable")
    }

    @Test
    fun `demonstrates comprehensive JSON extraction from complex text`() {
        val complexText = """
            Analysis Results:
            
            First, here's a summary object:
            {"summary": "Analysis complete", "status": "success"}
            
            Now here are the detailed results as an array:
            [
                {"id": 1, "score": 0.95},
                {"id": 2, "score": 0.87},
                {"id": 3, "score": 0.92}
            ]
            
            And here's some JSON in a code block:
            ```json
            {
                "metadata": {
                    "version": "1.0",
                    "timestamp": "2024-01-01T00:00:00Z"
                }
            }
            ```
            
            Finally, some inline JSON: `{"inline": true}`
        """.trimIndent()
        
        // Should extract the first JSON found
        val first = JsonResponseProcessor.extractFirstValidJson(complexText)
        assertNotNull(first)
        
        // Should extract all JSON elements
        val all = JsonResponseProcessor.extractAllValidJson(complexText)
        assertTrue(all.size >= 3, "Should find multiple JSON elements")
        
        // Should contain different types
        val hasObject = all.any { it is kotlinx.serialization.json.JsonObject }
        val hasArray = all.any { it is kotlinx.serialization.json.JsonArray }
        assertTrue(hasObject, "Should find JSON objects")
        assertTrue(hasArray, "Should find JSON arrays")
    }

    @Test
    fun `demonstrates configuration-based validation`() {
        val objectResult = JsonGenerationResult(
            responseText = """{"test": "object"}""",
            jsonElement = kotlinx.serialization.json.Json.parseToJsonElement("""{"test": "object"}"""),
            hasValidJson = true,
            attemptNumber = 1,
            extractedJsonText = """{"test": "object"}"""
        )
        
        val arrayResult = JsonGenerationResult(
            responseText = """[1, 2, 3]""",
            jsonElement = kotlinx.serialization.json.Json.parseToJsonElement("""[1, 2, 3]"""),
            hasValidJson = true,
            attemptNumber = 1,
            extractedJsonText = """[1, 2, 3]"""
        )
        
        // Test different configuration requirements
        assertTrue(objectResult.isValid(JsonRetryConfig())) // Default - any JSON is valid
        assertTrue(objectResult.isValid(JsonRetryConfig(requireType = JsonRetryConfig.JsonType.OBJECT)))
        assertFalse(objectResult.isValid(JsonRetryConfig(requireType = JsonRetryConfig.JsonType.ARRAY)))
        
        assertTrue(arrayResult.isValid(JsonRetryConfig(requireType = JsonRetryConfig.JsonType.ARRAY)))
        assertFalse(arrayResult.isValid(JsonRetryConfig(requireType = JsonRetryConfig.JsonType.OBJECT)))
    }

    @Test
    fun `demonstrates error handling and edge cases`() {        
        // Empty text
        assertNull(JsonResponseProcessor.extractFirstValidJson(""))
        
        // Text without JSON
        assertNull(JsonResponseProcessor.extractFirstValidJson("Just plain text"))
        
        // Valid JSON detection works
        assertTrue(JsonResponseProcessor.containsValidJson("""{"valid": true}"""))
        
        // Test with truly malformed JSON (missing closing brace)
        val malformed1 = JsonResponseProcessor.formatJson("""{"unclosed": "object" """)
        assertNull(malformed1, "Should not be able to format JSON with missing closing brace")
        
        // Test with invalid syntax
        val malformed2 = JsonResponseProcessor.formatJson("""{"key":}""")
        assertNull(malformed2, "Should not be able to format JSON with missing value")
    }
}