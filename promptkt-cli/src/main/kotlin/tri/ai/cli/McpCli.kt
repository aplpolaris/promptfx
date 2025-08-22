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
package tri.ai.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import tri.ai.prompt.PromptLibrary
import tri.ai.prompt.server.LocalMcpServerAdapter
import tri.ai.prompt.server.McpServerAdapter
import tri.ai.prompt.server.McpServerException
import tri.ai.prompt.server.RemoteMcpServerAdapter
import kotlin.system.exitProcess

fun main(args: Array<String>) =
    McpCli().main(args)

/**
 * Command-line interface for interacting with MCP (Model Context Protocol) prompt servers.
 * Supports both local and remote MCP servers.
 */
class McpCli : CliktCommand(
    name = "mcp-prompt",
    help = "Interface to MCP prompt servers - list, fill, and execute prompts"
) {
    private val serverUrl by option("--server", "-s", help = "MCP server URL (use 'local' for local server)")
        .default("local")
    private val verbose by option("--verbose", "-v", help = "Verbose output").flag()

    override fun run() {
        // Parent command - show help if no subcommand provided
        if (currentContext.invokedSubcommand == null) {
            echo(getFormattedHelp())
        }
    }

    init {
        subcommands(ListCommand(), GetCommand(), ExecuteCommand())
    }

    private fun createAdapter(): McpServerAdapter {
        return if (serverUrl == "local") {
            if (verbose) echo("Using local MCP server with PromptLibrary")
            LocalMcpServerAdapter(PromptLibrary.INSTANCE)
        } else {
            if (verbose) echo("Connecting to remote MCP server: $serverUrl")
            RemoteMcpServerAdapter(serverUrl)
        }
    }

    /** List all available prompts. */
    inner class ListCommand : CliktCommand(
        name = "list",
        help = "List all available prompts from the MCP server"
    ) {
        override fun run() = runBlocking {
            val adapter = this@McpCli.createAdapter()
            try {
                val prompts = adapter.listPrompts()
                
                if (prompts.isEmpty()) {
                    echo("No prompts found.")
                    return@runBlocking
                }
                
                echo("Available prompts:")
                echo("=".repeat(50))
                
                prompts.forEach { prompt ->
                    echo("ID: ${prompt.id}")
                    echo("Name: ${prompt.name}")
                    if (prompt.title != null) echo("Title: ${prompt.title}")
                    if (prompt.description != null) echo("Description: ${prompt.description}")
                    
                    prompt.arguments?.let { args ->
                        if (args.isNotEmpty()) {
                            echo("Arguments:")
                            args.forEach { arg ->
                                val required = if (arg.required) " (required)" else " (optional)"
                                echo("  - ${arg.name}$required: ${arg.description}")
                            }
                        }
                    }
                    echo("-".repeat(30))
                }
            } catch (e: McpServerException) {
                echo("Error: ${e.message}", err = true)
                exitProcess(1)
            } finally {
                adapter.close()
            }
        }
    }

    /** Get a filled prompt with arguments. */
    inner class GetCommand : CliktCommand(
        name = "get",
        help = "Get a prompt filled with arguments"
    ) {
        private val promptName by argument(help = "Name or ID of the prompt to get")
        private val arguments by argument(help = "Arguments in key=value format").multiple()

        override fun run() = runBlocking {
            val adapter = this@McpCli.createAdapter()
            try {
                val args = this@McpCli.parseArguments(arguments)
                if (this@McpCli.verbose) {
                    echo("Getting prompt: $promptName")
                    if (args.isNotEmpty()) {
                        echo("With arguments: ${args.entries.joinToString { "${it.key}=${it.value}" }}")
                    }
                }
                
                val response = adapter.getPrompt(promptName, args)
                
                if (response.description != null) {
                    echo("Description: ${response.description}")
                    echo("-".repeat(40))
                }
                
                echo("Filled prompt:")
                response.messages.forEach { message ->
                    echo("Role: ${message.role}")
                    message.content?.forEach { part ->
                        when (part.partType) {
                            tri.ai.core.MPartType.TEXT -> echo("Text: ${part.text}")
                            else -> echo("${part.partType}: ${part.text ?: part}")
                        }
                    }
                }
            } catch (e: McpServerException) {
                echo("Error: ${e.message}", err = true)
                exitProcess(1)
            } finally {
                adapter.close()
            }
        }
    }

    /** Execute a prompt (fill and then potentially run with LLM). */
    inner class ExecuteCommand : CliktCommand(
        name = "execute", 
        help = "Execute a prompt - fill it with arguments and display the result"
    ) {
        private val promptName by argument(help = "Name or ID of the prompt to execute")
        private val arguments by argument(help = "Arguments in key=value format").multiple()

        override fun run() = runBlocking {
            val adapter = this@McpCli.createAdapter()
            try {
                val args = this@McpCli.parseArguments(arguments)
                if (this@McpCli.verbose) {
                    echo("Executing prompt: $promptName")
                    if (args.isNotEmpty()) {
                        echo("With arguments: ${args.entries.joinToString { "${it.key}=${it.value}" }}")
                    }
                }
                
                val response = adapter.getPrompt(promptName, args)
                
                echo("Executed prompt:")
                echo("=".repeat(50))
                
                if (response.description != null) {
                    echo("Description: ${response.description}")
                    echo("-".repeat(40))
                }
                
                response.messages.forEach { message ->
                    echo("Role: ${message.role}")
                    message.content?.forEach { part ->
                        when (part.partType) {
                            tri.ai.core.MPartType.TEXT -> echo(part.text ?: "")
                            else -> echo("${part.partType}: ${part.text ?: part}")
                        }
                    }
                    echo()
                }
                
                echo("=".repeat(50))
                echo("Note: This shows the filled prompt. To run with an LLM, pipe to another tool or use a different command.")
                
            } catch (e: McpServerException) {
                echo("Error: ${e.message}", err = true)
                exitProcess(1)
            } finally {
                adapter.close()
            }
        }
    }

    private fun parseArguments(args: List<String>): Map<String, String> {
        return args.associate { arg ->
            val parts = arg.split('=', limit = 2)
            if (parts.size != 2) {
                throw IllegalArgumentException("Invalid argument format: '$arg'. Use key=value format.")
            }
            parts[0] to parts[1]
        }
    }
}