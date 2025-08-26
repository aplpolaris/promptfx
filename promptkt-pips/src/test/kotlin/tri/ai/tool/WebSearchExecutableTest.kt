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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import tri.ai.pips.core.ExecContext
import tri.ai.pips.core.MAPPER

class WebSearchExecutableTest {

    @Test
    fun testWebSearchExecutableProperties() {
        val searchTool = WebSearchExecutable()
        
        assertEquals("web_search", searchTool.name)
        assertEquals("Search the web using DuckDuckGo and return a list of relevant results with titles, URLs, and descriptions.", searchTool.description)
        assertEquals("1.0.0", searchTool.version)
        assertNotNull(searchTool.inputSchema)
        assertNotNull(searchTool.outputSchema)
    }

    @Test
    fun testWebSearchExecutableSchemas() {
        val searchTool = WebSearchExecutable()
        
        // Check that input schema is parsed correctly
        val inputSchema = searchTool.inputSchema!!
        assertEquals("object", inputSchema.get("type").asText())
        assertNotNull(inputSchema.get("properties"))
        assertNotNull(inputSchema.get("properties").get("query"))
        
        // Check that output schema exists
        val outputSchema = searchTool.outputSchema!!
        assertEquals("object", outputSchema.get("type").asText())
        assertNotNull(outputSchema.get("properties").get("result"))
    }

    @Test
    fun testWebSearchExecutableExecution() = runTest {
        val searchTool = WebSearchExecutable()
        val context = ExecContext()
        
        // Test with simple search query - we can't easily test actual web search without network
        // but we can test that the interface works correctly
        val inputJson = MAPPER.createObjectNode().put("query", "test query")
        val result = searchTool.execute(inputJson, context)
        
        // Should return a JSON result with query and results (even if search fails)
        assertNotNull(result.get("result"))
        
        // Clean up
        searchTool.close()
    }
}