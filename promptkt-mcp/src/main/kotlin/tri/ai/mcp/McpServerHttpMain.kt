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

import kotlinx.coroutines.runBlocking
import tri.ai.mcp.tool.StarterToolLibrary
import tri.ai.prompt.PromptLibrary

/**
 * Main class to run the MCP server over HTTP.
 * This server exposes Model Context Protocol endpoints via HTTP.
 */
fun main(args: Array<String>) {
    val port = args.firstOrNull()?.toIntOrNull() ?: 8080
    
    println("Starting MCP HTTP Server...")
    println("=".repeat(50))
    
    // Load prompts and tools
    // Create a new library instance and populate it with research-related prompts from the singleton
    val prompts = PromptLibrary().apply {
        PromptLibrary.INSTANCE
            .list { it.category?.startsWith("research") == true }
            .forEach { addPrompt(it) }
    }
    val tools = StarterToolLibrary()
    
    // Create embedded server with prompts and tools
    val adapter = McpServerEmbedded(prompts, tools)
    
    // Create and start HTTP server
    val server = McpServerHttp(adapter, port)
    server.startServer()
    
    println("✓ MCP HTTP Server is running!")
    println("=".repeat(50))
    println("Server URL: http://localhost:$port")
    println("MCP Endpoint: http://localhost:$port/mcp")
    println("Health Check: http://localhost:$port/health")
    println()
    println("Available prompts: ${prompts.list().size}")
    println("Available tools: ${runBlocking { tools.listTools().size }}")
    println()
    println("Press Ctrl+C to stop the server")
    println("=".repeat(50))
    
    // Keep the main thread alive
    Thread.currentThread().join()
}
