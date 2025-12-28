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
package tri.ai.mcp.stdio

import kotlinx.coroutines.runBlocking
import tri.ai.mcp.McpProviderEmbedded
import tri.ai.mcp.tool.McpToolLibraryStarter
import tri.ai.prompt.PromptLibrary

/**
 * Main class to run the MCP server over stdio.
 * This server exposes Model Context Protocol endpoints via standard input/output.
 */
fun main(args: Array<String>) {
    runBlocking {
        System.err.println("Starting MCP Stdio Server...")
        System.err.println("=".repeat(50))

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

        // Create and start stdio server
        val server = McpServerStdio(provider)

        System.err.println("✓ MCP Stdio Server is running!")
        System.err.println("=".repeat(50))
        System.err.println("Available prompts: ${prompts.list().size}")
        System.err.println("Available tools: ${tools.listTools().size}")
        System.err.println()
        System.err.println("Reading from stdin, writing to stdout...")
        System.err.println("Send JSON-RPC 2.0 requests to communicate with the server")
        System.err.println("=".repeat(50))

        try {
            // Start the blocking stdio loop
            server.startServer(System.`in`, System.out)
        } finally {
            System.err.println("\nShutting down MCP Stdio Server...")
            server.close()
            System.err.println("Server stopped.")
        }
    }
}
