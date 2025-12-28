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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import tri.ai.core.TextPlugin
import tri.ai.mcp.McpProvider
import tri.ai.mcp.McpProviderEmbedded
import tri.ai.mcp.McpException
import tri.ai.mcp.McpProviderHttp
import tri.ai.mcp.McpProviderRegistry
import tri.ai.mcp.stdio.McpServerStdio
import tri.ai.mcp.tool.McpContent
import tri.ai.mcp.tool.McpToolLibraryStarter
import tri.ai.openai.OpenAiModelIndex.GPT35_TURBO_ID
import tri.ai.prompt.PromptLibrary
import tri.util.ANSI_BOLD
import tri.util.ANSI_GRAY
import tri.util.ANSI_RESET
import kotlin.system.exitProcess

fun main(args: Array<String>) =
    McpCli().main(args)

/**
 * Command-line interface for interacting with MCP (Model Context Protocol) servers.
 * Supports both embedded and remote MCP providers.
 */
class McpCli : CliktCommand(
    name = "mcp-fx",
    help = "Interface to MCP servers - list, fill, and execute prompts, tools, and resources, or start an embedded server"
) {
    private val serverUrl by option("--server", "-s", help = "MCP server URL or name from registry (use 'embedded' for embedded server)")
        .default("embedded")
    private val registryConfig by option("--registry", "-r", help = "Path to MCP server registry configuration file (JSON or YAML)")
    private val promptLibrary by option("--prompt-library", "-p", help = "Custom prompt library file or directory path (for embedded server only)")
    private val toolLibrary by option("--tool-library", "-t", help = "FUTURE TBD -- Custom tool library file or directory path (for embedded server only)")
    private val verbose by option("--verbose", "-v", help = "Verbose output").flag("--no-verbose", default = false)
    
    private val registry: McpProviderRegistry by lazy {
        if (registryConfig != null) {
            if (verbose) echo("Loading MCP server registry from: $registryConfig")
            McpProviderRegistry.loadFromFile(registryConfig!!)
        } else {
            McpProviderRegistry.default()
        }
    }

    override fun run() {
        // Parent command - show help if no subcommand provided
        if (currentContext.invokedSubcommand == null) {
            echo(getFormattedHelp())
        }
    }

    init {
        subcommands(
            PromptListCommand(), PromptGetCommand(), PromptExecuteCommand(),
            ToolListCommand(), ToolExecuteCommand(),
            ResourceListCommand(), ResourceTemplatesListCommand(), ResourceReadCommand(),
            ServeCommand()
        )
    }

    //region INIT

    private fun createAdapter(): McpProvider {
        // First try to get from registry
        val fromRegistry = registry.getProvider(serverUrl)
        if (fromRegistry != null) {
            if (verbose) echo("Using MCP server from registry: $serverUrl")
            return fromRegistry
        }
        
        // Fallback to backward compatibility: treat as direct server specification
        return if (serverUrl == "embedded" || serverUrl == "local") {
            if (verbose) echo("Using in-memory MCP server: $serverUrl")
            val library = loadPromptLibrary()
            val toolLibrary = loadToolLibrary()
            McpProviderEmbedded(library, toolLibrary)
        } else {
            if (verbose) echo("Connecting to remote MCP server: $serverUrl")
            McpProviderHttp(serverUrl)
        }
    }

    private fun loadPromptLibrary(): PromptLibrary {
        return if (promptLibrary != null) {
            if (verbose) echo("Loading custom prompt library from: $promptLibrary")
            PromptLibrary.loadFromPath(promptLibrary!!)
        } else {
            if (verbose) echo("Using default embedded MCP server with PromptLibrary")
            PromptLibrary().apply {
                PromptLibrary.INSTANCE
                    .list { it.category?.startsWith("research") == true }
                    .forEach { addPrompt(it) }
            }
        }
    }

    private fun loadToolLibrary() = McpToolLibraryStarter()

    //endregion

    //region PROMPTS

    /** List all available prompts. */
    inner class PromptListCommand : CliktCommand(
        name = "prompts-list",
        help = "List all available prompts in the MCP server"
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
            } catch (e: McpException) {
                echo("Error: ${e.message}", err = true)
                exitProcess(1)
            } finally {
                adapter.close()
            }
        }
    }

    /** Get a filled prompt with arguments. */
    inner class PromptGetCommand : CliktCommand(
        name = "prompts-get",
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
            } catch (e: McpException) {
                echo("Error: ${e.message}", err = true)
                exitProcess(1)
            } finally {
                adapter.close()
            }
        }
    }

    /** Execute a prompt (fill and then potentially run with LLM). */
    inner class PromptExecuteCommand : CliktCommand(
        name = "prompts-execute",
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
                        throw McpException(
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

                    model.close()
                } catch (e: McpException) {
                    echo("Error: ${e.message}", err = true)
                    exitProcess(1)
                } finally {
                    adapter.close()
                }
            }
        }
    }

    //endregion

    //region TOOLS

    /** List all available tools. */
    inner class ToolListCommand : CliktCommand(
        name = "tools-list",
        help = "List all available tools from in the MCP server"
    ) {
        override fun run() = runBlocking {
            val adapter = this@McpCli.createAdapter()
            try {
                val tools = adapter.listTools()

                if (tools.isEmpty()) {
                    echo("No tools found.")
                    return@runBlocking
                }

                echo()
                echo("Available tools on $adapter:")
                echo("=".repeat(50))

                tools.forEach { tool ->
                    echo("${ANSI_BOLD}Name$ANSI_RESET: ${tool.name}")
                    echo("${ANSI_BOLD}Description$ANSI_RESET: $ANSI_GRAY${tool.description}$ANSI_RESET")
                    this@McpCli.printSchema(tool.inputSchema, "Input parameters")
                    this@McpCli.printSchema(tool.outputSchema, "Output properties")
                    echo("-".repeat(30))
                }
            } catch (e: McpException) {
                echo("Error: ${e.message}", err = true)
                exitProcess(1)
            } finally {
                adapter.close()
            }
        }
    }

    private fun printSchema(schema: JsonNode?, label: String) {
        if (schema != null && schema is ObjectNode) {
            val properties = schema.get("properties") as? ObjectNode
            val required = schema.get("required")?.mapNotNull { it.asText() }?.toSet() ?: emptySet()
            if (properties != null) {
                echo("${ANSI_BOLD}$label$ANSI_RESET:")
                properties.fields().forEach { (propName, propSchema) ->
                    val propDesc = propSchema.get("description")?.asText() ?: "No description"
                    val isRequired = if (required.contains(propName)) " (required)" else " (optional)"
                    echo("  - ${propName}$isRequired: $ANSI_GRAY${propDesc}$ANSI_RESET")
                }
            }
        }
    }

    /** Execute a tool with input. */
    inner class ToolExecuteCommand : CliktCommand(
        name = "tools-execute",
        help = "Execute a tool with input and display the result"
    ) {
        private val toolName by argument(help = "Name or ID of the tool to execute")
        private val arguments by argument(help = "Arguments in key=value format").multiple()

        override fun run() = runBlocking {
            val adapter = this@McpCli.createAdapter()
            try {
                val args = this@McpCli.parseArguments(arguments)
                if (this@McpCli.verbose) {
                    echo("Executing tool: $toolName")
                    if (args.isNotEmpty()) {
                        echo("With arguments: ${args.entries.joinToString { "${it.key}=${it.value}" }}")
                    }
                }
                val tool = adapter.getTool(toolName)
                if (tool == null) {
                    throw McpException("Tool with name '$toolName' not found.")
                }

                val result = adapter.callTool(toolName, args)
                if (this@McpCli.verbose) {
                    echo("=".repeat(50))
                    echo("Response from tool ${toolName}:")
                }
                if (result.isError == true) {
                    throw McpException("Tool execution error: ${result.errorMessage()}")
                }
                if (this@McpCli.verbose && result.metadata != null) {
                    echo("Response Metadata: ${result.metadata}")
                }

                if (result.content.isEmpty()) {
                    echo("No output received from the tool.")
                } else {
                    if (this@McpCli.verbose)
                        echo("Tool Output:")
                    val output = result.content
                    output.forEach { content ->
                        when (content) {
                            is McpContent.Text -> echo(content.text)
                            is McpContent.Image -> echo("Image (MIME: ${content.mimeType}, base64 length: ${content.data.length})")
                            is McpContent.Audio -> echo("Audio (MIME: ${content.mimeType}, base64 length: ${content.data.length})")
                            is McpContent.ResourceLink -> {
                                echo("Resource Link:")
                                echo("  URI: ${content.uri}")
                                echo("  Name: ${content.name}")
                                if (content.title != null) echo("  Title: ${content.title}")
                                if (content.description != null) echo("  Description: ${content.description}")
                                if (content.mimeType != null) echo("  MIME Type: ${content.mimeType}")
                            }
                            is McpContent.Resource -> {
                                echo("Embedded Resource:")
                                echo("  URI: ${content.uri}")
                                echo("  Text: ${content.text}")
                                if (content.mimeType != null) echo("  MIME Type: ${content.mimeType}")
                            }
                        }
                    }
                }
            } catch (e: McpException) {
                echo("Error: ${e.message}", err = true)
                exitProcess(1)
            } finally {
                adapter.close()
            }
        }
    }

    //endregion

    //region RESOURCES

    /** List all available resources. */
    inner class ResourceListCommand : CliktCommand(
        name = "resources-list",
        help = "List all available resources from the MCP server"
    ) {
        override fun run() = runBlocking {
            val adapter = this@McpCli.createAdapter()
            try {
                val resources = adapter.listResources()

                if (resources.isEmpty()) {
                    echo("No resources found.")
                    return@runBlocking
                }

                echo()
                echo("Available resources on $adapter:")
                echo("=".repeat(50))

                resources.forEach { resource ->
                    echo("${ANSI_BOLD}URI$ANSI_RESET: ${resource.uri}")
                    echo("${ANSI_BOLD}Name$ANSI_RESET: ${resource.name}")
                    if (resource.description != null) {
                        echo("${ANSI_BOLD}Description$ANSI_RESET: $ANSI_GRAY${resource.description}$ANSI_RESET")
                    }
                    if (resource.mimeType != null) {
                        echo("${ANSI_BOLD}MIME Type$ANSI_RESET: ${resource.mimeType}")
                    }
                    echo("-".repeat(30))
                }
            } catch (e: McpException) {
                echo("Error: ${e.message}", err = true)
                exitProcess(1)
            } finally {
                adapter.close()
            }
        }
    }

    /** List all available resource templates. */
    inner class ResourceTemplatesListCommand : CliktCommand(
        name = "resources-templates-list",
        help = "List all available resource templates from the MCP server"
    ) {
        override fun run() = runBlocking {
            val adapter = this@McpCli.createAdapter()
            try {
                val templates = adapter.listResourceTemplates()

                if (templates.isEmpty()) {
                    echo("No resource templates found.")
                    return@runBlocking
                }

                echo()
                echo("Available resource templates on $adapter:")
                echo("=".repeat(50))

                templates.forEach { template ->
                    echo("${ANSI_BOLD}URI Template$ANSI_RESET: ${template.uriTemplate}")
                    echo("${ANSI_BOLD}Name$ANSI_RESET: ${template.name}")
                    if (template.description != null) {
                        echo("${ANSI_BOLD}Description$ANSI_RESET: $ANSI_GRAY${template.description}$ANSI_RESET")
                    }
                    if (template.mimeType != null) {
                        echo("${ANSI_BOLD}MIME Type$ANSI_RESET: ${template.mimeType}")
                    }
                    echo("-".repeat(30))
                }
            } catch (e: McpException) {
                echo("Error: ${e.message}", err = true)
                exitProcess(1)
            } finally {
                adapter.close()
            }
        }
    }

    /** Read a resource by URI. */
    inner class ResourceReadCommand : CliktCommand(
        name = "resources-read",
        help = "Read a resource by URI"
    ) {
        private val uri by argument(help = "URI of the resource to read")

        override fun run() = runBlocking {
            val adapter = this@McpCli.createAdapter()
            try {
                if (this@McpCli.verbose) {
                    echo("Reading resource: $uri")
                }

                val response = adapter.readResource(uri)

                echo()
                echo("Resource contents:")
                echo("=".repeat(50))

                response.contents.forEach { content ->
                    echo("${ANSI_BOLD}URI$ANSI_RESET: ${content.uri}")
                    if (content.mimeType != null) {
                        echo("${ANSI_BOLD}MIME Type$ANSI_RESET: ${content.mimeType}")
                    }
                    if (content.text != null) {
                        echo("${ANSI_BOLD}Text Content$ANSI_RESET:")
                        echo(content.text)
                    }
                    content.blob?.let { blob ->
                        echo("${ANSI_BOLD}Binary Content$ANSI_RESET: (base64 encoded, ${blob.length} characters)")
                        if (this@McpCli.verbose) {
                            echo(blob)
                        }
                    }
                    echo("-".repeat(30))
                }
            } catch (e: McpException) {
                echo("Error: ${e.message}", err = true)
                exitProcess(1)
            } finally {
                adapter.close()
            }
        }
    }

    //endregion

    /** Starts an MCP server on stdio. */
    inner class ServeCommand : CliktCommand(
        name = "start",
        help = "Start an MCP server on stdio, with embedded prompts and tools"
    ) {
        override fun run() {
            runBlocking {
                val prompts = this@McpCli.loadPromptLibrary()
                val tools = this@McpCli.loadToolLibrary()
                val locServer = McpProviderEmbedded(prompts, tools)
                McpServerStdio(locServer).startServer(System.`in`, System.out)
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

