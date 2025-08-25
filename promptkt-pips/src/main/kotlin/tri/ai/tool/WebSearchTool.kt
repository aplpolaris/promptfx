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

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import tri.ai.core.CompletionBuilder
import java.net.URLDecoder

/**
 * A web search tool that uses DuckDuckGo to search the web and return structured results.
 * This tool provides access to web search capabilities without requiring API keys.
 */
class WebSearchTool : JsonTool(
    name = "web_search",
    description = "Search the web using DuckDuckGo and return a list of relevant results with titles, URLs, and descriptions.",
    jsonSchema = """{
        "type": "object",
        "properties": {
            "query": {
                "type": "string",
                "description": "The search query to execute"
            },
            "max_results": {
                "type": "integer",
                "description": "Maximum number of results to return (default: 5, max: 10)",
                "minimum": 1,
                "maximum": 10
            }
        },
        "required": ["query"]
    }"""
) {

    private val httpClient = HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(true)
            }
        }
    }

    /**
     * Executes a web search using DuckDuckGo and returns structured results.
     */
    override suspend fun run(input: JsonObject): String {
        val query = input["query"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Query parameter is required")
        
        val maxResults = input["max_results"]?.jsonPrimitive?.intOrNull ?: 5
        val clampedMaxResults = maxResults.coerceIn(1, 10)

        return try {
            val searchResults = performDuckDuckGoSearch(query, clampedMaxResults)
            CompletionBuilder.JSON_MAPPER.writeValueAsString(mapOf(
                "query" to query,
                "results" to searchResults
            ))
        } catch (e: Exception) {
            CompletionBuilder.JSON_MAPPER.writeValueAsString(mapOf(
                "query" to query,
                "error" to "Search failed: ${e.message}",
                "results" to emptyList<SearchResult>()
            ))
        }
    }

    /**
     * Performs a search using DuckDuckGo by scraping HTML results.
     */
    private suspend fun performDuckDuckGoSearch(query: String, maxResults: Int): List<SearchResult> {
        val encodedQuery = query.replace(" ", "+")
        val searchUrl = "https://html.duckduckgo.com/html/?q=$encodedQuery"
        
        val response = httpClient.get(searchUrl) {
            headers {
                append(HttpHeaders.UserAgent, "Mozilla/5.0 (compatible; WebSearchTool/1.0)")
                append(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                append(HttpHeaders.AcceptLanguage, "en-US,en;q=0.5")
            }
        }

        if (!response.status.isSuccess()) {
            throw RuntimeException("Search request failed with status: ${response.status}")
        }

        val html = response.bodyAsText()
        return parseSearchResults(html, maxResults)
    }

    /**
     * Parses DuckDuckGo HTML search results using regular expressions.
     */
    private fun parseSearchResults(html: String, maxResults: Int): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        
        // Simple regex patterns to extract search results
        // This is a simplified approach that looks for result blocks
        val resultPattern = Regex("""<div[^>]*class[^>]*result[^>]*>(.*?)</div>(?=\s*<div[^>]*class[^>]*result|\s*</body|\s*$)""", RegexOption.DOT_MATCHES_ALL)
        val titleUrlPattern = Regex("""<a[^>]*class[^>]*result__a[^>]*href="([^"]*)"[^>]*>([^<]*)</a>""")
        val snippetPattern = Regex("""<a[^>]*class[^>]*result__snippet[^>]*>([^<]*)</a>""")
        
        val matches = resultPattern.findAll(html)
        
        for (match in matches.take(maxResults)) {
            val resultBlock = match.groupValues[1]
            
            val titleUrlMatch = titleUrlPattern.find(resultBlock)
            val snippetMatch = snippetPattern.find(resultBlock)
            
            if (titleUrlMatch != null && titleUrlMatch.groupValues.size >= 3) {
                val url = titleUrlMatch.groupValues[1].trim()
                val title = titleUrlMatch.groupValues[2].trim()
                val description = snippetMatch?.groupValues?.get(1)?.trim() ?: ""
                val decodedUrl = URLDecoder.decode(url.substringAfter("uddg=").substringBefore("&amp;rut="), "UTF-8")

                if (title.isNotEmpty() && url.isNotEmpty()) {
                    results.add(SearchResult(title, decodedUrl, description))
                }
            }
        }
        
        return results
    }

    /**
     * Closes the HTTP client to clean up resources.
     */
    fun close() {
        httpClient.close()
    }
}

/**
 * Represents a single search result.
 */
data class SearchResult(
    val title: String,
    val url: String,
    val description: String
)