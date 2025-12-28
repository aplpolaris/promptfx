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
package tri.ai.mcp.http

import kotlinx.coroutines.runBlocking
import tri.ai.mcp.McpProviderEmbedded
import tri.ai.mcp.tool.McpToolLibraryStarter
import tri.ai.prompt.PromptLibrary
import java.util.concurrent.CountDownLatch

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
        PromptLibrary.Companion.INSTANCE
            .list { it.category?.startsWith("research") == true }
            .forEach { addPrompt(it) }
    }
    val tools = McpToolLibraryStarter()

    // Create embedded server with prompts and tools
    val provider = McpProviderEmbedded(prompts, tools)

    // Create and start HTTP server
    val server = McpServerHttp(provider, port)
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

    // Use a latch to keep the server running and handle shutdown gracefully
    val shutdownLatch = CountDownLatch(1)

    // Add shutdown hook to handle Ctrl+C
    Runtime.getRuntime().addShutdownHook(Thread {
        println("\nShutting down MCP HTTP Server...")
        runBlocking {
            server.close()
        }
        println("Server stopped.")
        shutdownLatch.countDown()
    })

    // Wait for shutdown signal
    shutdownLatch.await()
}
