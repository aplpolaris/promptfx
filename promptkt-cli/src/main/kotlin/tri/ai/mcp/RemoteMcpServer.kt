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
package tri.ai.mcp

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * Remote MCP server adapter that connects to external MCP servers via HTTP.
 */
class RemoteMcpServer(private val baseUrl: String) : McpServerAdapter {
    
    private val httpClient = HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(true)
            }
        }
    }
    
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    
    override suspend fun listPrompts(): List<McpPrompt> {
        try {
            val response = httpClient.get("$baseUrl/prompts/list")
            if (response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                return objectMapper.readValue<List<McpPrompt>>(responseBody)
            } else {
                throw McpServerException("Failed to list prompts: ${response.status}")
            }
        } catch (e: Exception) {
            throw McpServerException("Error connecting to MCP server: ${e.message}")
        }
    }
    
    override suspend fun getPrompt(name: String, args: Map<String, String>): McpGetPromptResponse {
        try {
            val response = httpClient.post("$baseUrl/prompts/get") {
                contentType(ContentType.Application.Json)
                setBody(objectMapper.writeValueAsString(mapOf(
                    "name" to name,
                    "arguments" to args
                )))
            }
            
            if (response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                return objectMapper.readValue<McpGetPromptResponse>(responseBody)
            } else {
                throw McpServerException("Failed to get prompt '$name': ${response.status}")
            }
        } catch (e: Exception) {
            throw McpServerException("Error getting prompt from MCP server: ${e.message}")
        }
    }
    
    override suspend fun getCapabilities(): McpServerCapabilities? {
        try {
            val response = httpClient.get("$baseUrl/capabilities")
            if (response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                return objectMapper.readValue<McpServerCapabilities>(responseBody)
            } else {
                // Capabilities endpoint is optional, so we don't throw an error
                return null
            }
        } catch (e: Exception) {
            // Capabilities endpoint is optional, so we don't throw an error
            return null
        }
    }
    
    override suspend fun close() {
        httpClient.close()
    }
}