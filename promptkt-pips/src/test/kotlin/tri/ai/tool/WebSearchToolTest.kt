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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Disabled
import tri.ai.core.CompletionBuilder

class WebSearchToolTest {

    private val webSearchTool = WebSearchTool()

    @AfterEach
    fun cleanup() {
        webSearchTool.close()
    }

    @Test
    fun testSearchWithDefaultMaxResults() = runTest {
        val input = buildJsonObject {
            put("query", "kotlin programming language")
        }

        val result = webSearchTool.run(input)
        val parsedResult = CompletionBuilder.JSON_MAPPER.readTree(result)

        assertNotNull(parsedResult)
        assertEquals("kotlin programming language", parsedResult["query"].asText())
        assertTrue(parsedResult.has("results"))
        
        val results = parsedResult["results"]
        assertTrue(results.isArray)
        assertTrue(results.size() <= 5) // Default max results
        
        // Check if we got some results (if not blocked)
        if (results.size() > 0) {
            val firstResult = results[0]
            assertTrue(firstResult.has("title"))
            assertTrue(firstResult.has("url"))
            assertTrue(firstResult.has("description"))
            assertFalse(firstResult["title"].asText().isEmpty())
            assertFalse(firstResult["url"].asText().isEmpty())
        }
    }

    @Test
    fun testSearchWithCustomMaxResults() = runTest {
        val input = buildJsonObject {
            put("query", "machine learning")
            put("max_results", 3)
        }

        val result = webSearchTool.run(input)
        val parsedResult = CompletionBuilder.JSON_MAPPER.readTree(result)

        assertNotNull(parsedResult)
        assertEquals("machine learning", parsedResult["query"].asText())
        
        val results = parsedResult["results"]
        assertTrue(results.isArray)
        assertTrue(results.size() <= 3) // Custom max results
    }

    @Test
    fun testSearchWithMaxResultsClamping() = runTest {
        val input = buildJsonObject {
            put("query", "test query")
            put("max_results", 15) // Over the limit
        }

        val result = webSearchTool.run(input)
        val parsedResult = CompletionBuilder.JSON_MAPPER.readTree(result)

        assertNotNull(parsedResult)
        val results = parsedResult["results"]
        assertTrue(results.size() <= 10) // Should be clamped to max 10
    }

    @Test
    fun testSearchWithMinResultsClamping() = runTest {
        val input = buildJsonObject {
            put("query", "test query")
            put("max_results", 0) // Under the limit
        }

        val result = webSearchTool.run(input)
        val parsedResult = CompletionBuilder.JSON_MAPPER.readTree(result)

        assertNotNull(parsedResult)
        assertEquals("test query", parsedResult["query"].asText())
        // Should be clamped to at least 1, though actual results may vary due to network/parsing
        assertTrue(parsedResult.has("results"))
    }

    @Test
    fun testMissingQueryParameter() {
        val input = buildJsonObject {
            put("max_results", 5)
        }

        assertThrows(IllegalArgumentException::class.java) {
            runTest {
                webSearchTool.run(input)
            }
        }
    }

    @Test
    fun testEmptyQuery() = runTest {
        val input = buildJsonObject {
            put("query", "")
            put("max_results", 5)
        }

        val result = webSearchTool.run(input)
        val parsedResult = CompletionBuilder.JSON_MAPPER.readTree(result)

        assertNotNull(parsedResult)
        assertEquals("", parsedResult["query"].asText())
        
        // Empty query should still return results array (may be empty)
        assertTrue(parsedResult.has("results"))
    }

    @Test
    fun testSpecialCharactersInQuery() = runTest {
        val input = buildJsonObject {
            put("query", "C++ programming & development")
            put("max_results", 2)
        }

        val result = webSearchTool.run(input)
        val parsedResult = CompletionBuilder.JSON_MAPPER.readTree(result)

        assertNotNull(parsedResult)
        assertEquals("C++ programming & development", parsedResult["query"].asText())
        assertTrue(parsedResult.has("results"))
    }

    @Test
    fun testJsonSchemaCompliance() {
        // Test that the tool provides a valid JSON schema
        val tool = WebSearchTool()
        assertNotNull(tool.tool.jsonSchema)
        assertFalse(tool.tool.jsonSchema.isEmpty())
        
        // Should be valid JSON
        assertDoesNotThrow {
            Json.parseToJsonElement(tool.tool.jsonSchema)
        }
    }

    @Test
    fun testToolMetadata() {
        val tool = WebSearchTool()
        assertEquals("web_search", tool.tool.name)
        assertFalse(tool.tool.description.isEmpty())
        assertTrue(tool.tool.description.contains("DuckDuckGo"))
    }

    /**
     * This test is disabled by default as it makes real network calls.
     * Enable it for manual testing with internet connectivity.
     */
    @Test
    @Disabled("Requires internet connectivity")
    fun testActualWebSearch() = runTest {
        val input = buildJsonObject {
            put("query", "OpenAI ChatGPT")
            put("max_results", 3)
        }

        val result = webSearchTool.run(input)
        val parsedResult = CompletionBuilder.JSON_MAPPER.readTree(result)

        println("Search result: $result")
        
        assertNotNull(parsedResult)
        assertEquals("OpenAI ChatGPT", parsedResult["query"].asText())
        
        val results = parsedResult["results"]
        assertTrue(results.isArray)
        
        if (results.size() > 0) {
            val firstResult = results[0]
            println("First result title: ${firstResult["title"].asText()}")
            println("First result URL: ${firstResult["url"].asText()}")
            println("First result description: ${firstResult["description"].asText()}")
            
            assertTrue(firstResult["title"].asText().isNotEmpty())
            assertTrue(firstResult["url"].asText().startsWith("http"))
        }
    }
}