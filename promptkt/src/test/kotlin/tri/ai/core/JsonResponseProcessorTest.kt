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

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JsonResponseProcessorTest {

    @Test
    fun `should extract simple JSON object`() {
        val text = """{"name": "John", "age": 30}"""
        val result = JsonResponseProcessor.extractFirstValidJson(text)
        
        assertNotNull(result)
        assertTrue(result is JsonObject)
    }

    @Test
    fun `should extract simple JSON array`() {
        val text = """[1, 2, 3, "test"]"""
        val result = JsonResponseProcessor.extractFirstValidJson(text)
        
        assertNotNull(result)
        assertTrue(result is JsonArray)
    }

    @Test
    fun `should extract JSON from text with surrounding content`() {
        val text = """
            Here's the result you requested:
            {"status": "success", "data": [1, 2, 3]}
            Hope this helps!
        """.trimIndent()
        
        val result = JsonResponseProcessor.extractFirstValidJson(text)
        
        assertNotNull(result)
        assertTrue(result is JsonObject)
    }

    @Test
    fun `should extract JSON from code block`() {
        val text = """
            Here's the JSON:
            ```json
            {"message": "hello world", "count": 42}
            ```
            That's all!
        """.trimIndent()
        
        val result = JsonResponseProcessor.extractFirstValidJson(text)
        
        assertNotNull(result)
        assertTrue(result is JsonObject)
    }

    @Test
    fun `should extract JSON from generic code block`() {
        val text = """
            Here's the data:
            ```
            [{"id": 1}, {"id": 2}]
            ```
        """.trimIndent()
        
        val result = JsonResponseProcessor.extractFirstValidJson(text)
        
        assertNotNull(result)
        assertTrue(result is JsonArray)
    }

    @Test
    fun `should handle nested JSON objects`() {
        val text = """{"user": {"name": "John", "details": {"age": 30, "city": "NYC"}}}"""
        val result = JsonResponseProcessor.extractFirstValidJson(text)
        
        assertNotNull(result)
        assertTrue(result is JsonObject)
    }

    @Test
    fun `should return null for text without JSON`() {
        val text = "This is just plain text without any JSON content."
        val result = JsonResponseProcessor.extractFirstValidJson(text)
        
        assertNull(result)
    }

    @Test
    fun `should extract first JSON when multiple exist`() {
        val text = """
            First object: {"a": 1}
            Second object: {"b": 2}
        """.trimIndent()
        
        val result = JsonResponseProcessor.extractFirstValidJson(text)
        
        assertNotNull(result)
        assertTrue(result is JsonObject)
        // Should extract the first one
        assertEquals("""{"a":1}""", result.toString())
    }

    @Test
    fun `should extract all valid JSON elements`() {
        val text = """
            First: {"a": 1}
            Second: [1, 2, 3]
            Third: {"b": 2}
        """.trimIndent()
        
        val results = JsonResponseProcessor.extractAllValidJson(text)
        
        assertEquals(3, results.size)
        
        val objects = results.filterIsInstance<JsonObject>()
        val arrays = results.filterIsInstance<JsonArray>()
        
        assertEquals(2, objects.size) // Should have 2 objects
        assertEquals(1, arrays.size)  // Should have 1 array
        
        // Check that we have the expected content
        assertTrue(objects.any { it.toString() == "{\"a\":1}" })
        assertTrue(objects.any { it.toString() == "{\"b\":2}" })
        assertTrue(arrays.any { it.toString() == "[1,2,3]" })
    }

    @Test
    fun `should handle JSON with escaped quotes`() {
        val text = """{"message": "She said \"Hello world!\""}"""
        val result = JsonResponseProcessor.extractFirstValidJson(text)
        
        assertNotNull(result)
        assertTrue(result is JsonObject)
    }

    @Test
    fun `should detect valid JSON in text`() {
        assertTrue(JsonResponseProcessor.containsValidJson("""{"test": true}"""))
        assertTrue(JsonResponseProcessor.containsValidJson("""Text with {"json": "embedded"} here"""))
        assertTrue(JsonResponseProcessor.containsValidJson("""[1, 2, 3]"""))
        assertTrue(!JsonResponseProcessor.containsValidJson("Just plain text"))
        assertTrue(!JsonResponseProcessor.containsValidJson("{invalid json}"))
    }

    @Test
    fun `should extract raw JSON text`() {
        val text = """
            Here's your data:
            {"name": "Test", "value": 123}
            End of data.
        """.trimIndent()
        
        val jsonText = JsonResponseProcessor.extractFirstJsonText(text)
        
        assertNotNull(jsonText)
        assertEquals("""{"name": "Test", "value": 123}""", jsonText)
    }

    @Test
    fun `should format JSON with proper indentation`() {
        val jsonText = """{"name":"John","age":30,"city":"NYC"}"""
        val formatted = JsonResponseProcessor.formatJson(jsonText)
        
        assertNotNull(formatted)
        assertTrue(formatted!!.contains("  ")) // Should have indentation
        assertTrue(formatted.contains("\n")) // Should have newlines
    }

    @Test
    fun `should handle malformed JSON gracefully`() {
        val text = """{"name": "John", "age":}"""
        val result = JsonResponseProcessor.extractFirstValidJson(text)
        
        assertNull(result)
    }

    @Test
    fun `should handle unmatched braces gracefully`() {
        val text = """{"name": "John", "details": {"age": 30}"""
        val result = JsonResponseProcessor.extractFirstValidJson(text)
        
        assertNull(result)
    }

    @Test
    fun `should extract JSON from inline code`() {
        val text = """The result is `{"status": "ok"}` for this request."""
        val result = JsonResponseProcessor.extractFirstValidJson(text)
        
        assertNotNull(result)
        assertTrue(result is JsonObject)
    }
}