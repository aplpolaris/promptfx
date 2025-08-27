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
import tri.ai.core.TextPlugin
import tri.ai.gemini.GeminiMultimodalChat
import tri.ai.mcp.LocalMcpServer
import tri.ai.mcp.McpServerAdapter
import tri.ai.mcp.McpServerException
import tri.ai.mcp.RemoteMcpServer
import tri.ai.mcp.StdioMcpServer
import tri.ai.mcp.tool.StarterToolLibrary
import tri.ai.openai.OpenAiModelIndex.GPT35_TURBO_ID
import tri.ai.openai.OpenAiMultimodalChat
import tri.ai.prompt.PromptLibrary
import tri.util.ANSI_BOLD
import tri.util.ANSI_GRAY
import tri.util.ANSI_RESET
import kotlin.system.exitProcess

fun main(args: Array<String>) =
    McpCli().main(args)

/**
 * Command-line interface for interacting with MCP (Model Context Protocol) prompt servers.
 * Supports both local and remote MCP servers.
 */
class McpCli : CliktCommand(
    name = "mcp-prompt",
    help = "Interface to MCP prompt servers - list, fill, and execute prompts, or start a local server"
) {
    private val serverUrl by option("--server", "-s", help = "MCP server URL (use 'local' for local server)")
        .default("local")
    private val promptLibrary by option("--prompt-library", "-p", help = "Custom prompt library file or directory path (for local server only)")
    private val verbose by option("--verbose", "-v", help = "Verbose output").flag()

    override fun run() {
        // Parent command - show help if no subcommand provided
        if (currentContext.invokedSubcommand == null) {
            echo(getFormattedHelp())
        }
    }

    init {
        subcommands(ListCommand(), GetCommand(), ExecuteCommand(), ServeCommand())
    }

    private fun createAdapter(): McpServerAdapter {
        return if (serverUrl == "local") {
            val library = loadPromptLibrary()
            val toolLibrary = StarterToolLibrary()
            LocalMcpServer(library, toolLibrary)
        } else {
            if (verbose) echo("Connecting to remote MCP server: $serverUrl")
            RemoteMcpServer(serverUrl)
        }
    }

    private fun loadPromptLibrary(): PromptLibrary {
        return if (promptLibrary != null) {
            if (verbose) echo("Loading custom prompt library from: $promptLibrary")
            PromptLibrary.loadFromPath(promptLibrary!!)
        } else {
            if (verbose) echo("Using default local MCP server with PromptLibrary")
            PromptLibrary.INSTANCE
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

                echo()
                echo("Available prompts on $adapter:")
                echo("=".repeat(50))
                
                prompts.forEach { prompt ->
                    echo("${ANSI_BOLD}Name$ANSI_RESET: ${prompt.name}")
                    if (prompt.title != null) echo("${ANSI_BOLD}Title$ANSI_RESET: ${prompt.title}")
                    if (prompt.description != null) echo("${ANSI_BOLD}Description$ANSI_RESET: $ANSI_GRAY${prompt.description}$ANSI_RESET")
                    
                    prompt.arguments?.let { args ->
                        if (args.isNotEmpty()) {
                            echo("${ANSI_BOLD}Arguments$ANSI_RESET:")
                            args.forEach { arg ->
                                val required = if (arg.required) " (required)" else " (optional)"
                                echo("  - ${arg.name}$required: $ANSI_GRAY${arg.description}$ANSI_RESET")
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
        help = "Execute a prompt - fill it with arguments and display the result after calling an LLM"
    ) {
        private val model by option("--model", "-m", help = "Chat model or LLM to use (default $GPT35_TURBO_ID)")
            .default(GPT35_TURBO_ID)
        private val promptName by argument(help = "Name or ID of the prompt to execute")
        private val arguments by argument(help = "Arguments in key=value format").multiple()

        override fun run() {
            runBlocking {
                val adapter = this@McpCli.createAdapter()
                try {
                    val args = this@McpCli.parseArguments(arguments)
                    if (this@McpCli.verbose) {
                        echo("Executing prompt: $promptName")
                        if (args.isNotEmpty()) {
                            echo("With arguments: ${args.entries.joinToString { "${it.key}=${it.value}" }}")
                        }
                    }
                    
                    val filledPrompt = adapter.getPrompt(promptName, args)

                    if (this@McpCli.verbose) {
                        echo("Filled prompt:")
                        echo("=".repeat(50))
                        if (filledPrompt.description != null) {
                            echo("Description: ${filledPrompt.description}")
                            echo("-".repeat(40))
                        }
                    }


                    filledPrompt.messages.forEach { message ->
                        if (this@McpCli.verbose) {
                            if (filledPrompt.description != null) {
                                echo("Role: ${message.role}")
                            }
                        }
                        message.content?.forEach { part ->
                            when (part.partType) {
                                tri.ai.core.MPartType.TEXT -> echo(part.text ?: "")
                                else -> echo("${part.partType}: ${part.text ?: part}")
                            }
                        }
                        echo()
                    }

                    val model = try {
                        TextPlugin.multimodalModel(model)
                    } catch (x: NoSuchElementException) {
                        throw McpServerException(
                            "Model '$model' not found. Available models: ${
                                TextPlugin.chatModels().joinToString { it.modelId }
                            }", x
                        )
                    }

                    val completed = model.chat(filledPrompt.messages)

                    if (this@McpCli.verbose) {
                        echo("=".repeat(50))
                        echo("Response from model '${model.modelId}':")
                    }

                    if (completed.values == null) {
                        echo("No response received from the model.")
                    } else if (completed.values!!.isEmpty()) {
                        echo("Model returned an empty response.")
                    } else {
                        completed.values!!.forEach { message ->
                            val mm = message.multimodalMessage!!
                            if (this@McpCli.verbose) {
                                echo("Role: ${mm.role}")
                            }
                            mm.content?.forEach { part ->
                                when (part.partType) {
                                    tri.ai.core.MPartType.TEXT -> echo(part.text ?: "")
                                    else -> echo("${part.partType}: ${part.text ?: part}")
                                }
                            }
                            echo()
                        }
                    }

                    (model as? OpenAiMultimodalChat)?.client?.client?.close()
                    (model as? GeminiMultimodalChat)?.client?.close()
                } catch (e: McpServerException) {
                    echo("Error: ${e.message}", err = true)
                    exitProcess(1)
                } finally {
                    adapter.close()
                }
            }
        }
    }

    /** Starts an MCP server on stdio. */
    inner class ServeCommand : CliktCommand(
        name = "start",
        help = "Start an MCP server on stdio, with locally provided prompts"
    ) {
        override fun run() {
            runBlocking {
                val library = this@McpCli.loadPromptLibrary()
                val locServer = LocalMcpServer(library, StarterToolLibrary())
                StdioMcpServer(locServer).startServer(System.`in`, System.out)
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

