/*-
 * #%L
 * tri.promptfx:promptrt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import tri.ai.cli.config.ConfigLoader
import tri.ai.cli.repl.PromptRtRepl
import tri.ai.cli.repl.SessionState
import tri.ai.core.AiModelProvider
import tri.ai.core.MChatRole
import tri.ai.core.TextChatMessage
import tri.ai.prompt.trace.AiOutput
import java.io.File

class PromptRt : CliktCommand(name = "promptrt") {
    override val invokeWithoutSubcommand = true

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = PromptRt()
            .subcommands(PromptRtChatOnce(), PromptRtBatch(), PromptRtModels(), PromptRtProviders(), PromptRtShowConfig())
            .main(args)
    }

    private val configFile by option("--config", "-c", help = "Config file (default ~/.promptrt/config.yaml)")
        .file(mustExist = false)
        .default(File(System.getProperty("user.home"), ".promptrt/config.yaml"))

    private val mode by option("--mode", "-m", help = "Launch in a specific mode")

    override fun run() {
        if (currentContext.invokedSubcommand != null) return
        val config = ConfigLoader.load(configFile)
        val effectiveConfig = if (mode != null) config.copy(defaultMode = mode!!) else config
        PromptRtRepl(effectiveConfig).start()
    }
}

class PromptRtChatOnce : CliktCommand(name = "chat") {
    private val message by argument(help = "Message to send")
    private val mode by option("--mode", help = "Mode to use").default("plain")
    private val jsonOutput by option("--json", help = "Output as JSON").flag()

    override fun run() {
        val config = ConfigLoader.loadDefault()
        val state = SessionState.fromConfig(config.copy(defaultMode = mode))
        val model = try {
            AiModelProvider.chatModels().first { it.modelId == state.effectiveModel }
        } catch (e: NoSuchElementException) {
            System.err.println("Model '${state.effectiveModel}' not found.")
            System.err.println("Available: ${AiModelProvider.chatModels().map { it.modelId }.joinToString()}")
            return
        }
        val response = runBlocking {
            model.chat(listOf(TextChatMessage(MChatRole.User, message)))
        }
        val text = (response.firstValue as AiOutput.ChatMessage).message.content ?: ""
        if (jsonOutput) {
            val escaped = com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(text)
            println("""{"response":$escaped}""")
        } else {
            println(text)
        }
    }
}

class PromptRtBatch : CliktCommand(name = "batch") {
    override fun run() = echo("[stub] batch not yet implemented")
}

class PromptRtModels : CliktCommand(name = "models") {
    override fun run() {
        val chat = AiModelProvider.chatModels()
        println("Chat models (${chat.size}):")
        chat.forEach { println("  ${it.modelId}") }
        val embed = AiModelProvider.embeddingModels()
        if (embed.isNotEmpty()) {
            println("\nEmbedding models (${embed.size}):")
            embed.forEach { println("  ${it.modelId}") }
        }
    }
}

class PromptRtProviders : CliktCommand(name = "providers") {
    override fun run() {
        val plugins = AiModelProvider.orderedPlugins
        if (plugins.isEmpty()) {
            println("No providers loaded. Check API key environment variables.")
            return
        }
        println("Loaded providers (${plugins.size}):")
        plugins.forEach { p -> println("  ${p.javaClass.simpleName}") }
        println("\nTotal chat models:      ${AiModelProvider.chatModels().size}")
        println("Total embedding models: ${AiModelProvider.embeddingModels().size}")
    }
}

class PromptRtShowConfig : CliktCommand(name = "config") {
    override fun run() {
        val config = ConfigLoader.loadDefault()
        val configFile = File(System.getProperty("user.home"), ".promptrt/config.yaml")
        println("Config file: ${configFile.absolutePath}")
        println("Exists:      ${configFile.exists()}")
        println("Default mode: ${config.defaultMode}")
        println("Modes: ${config.allModeNames.joinToString(", ")}")
        println("Providers configured: ${config.providers.keys.joinToString(", ").ifEmpty { "(none)" }}")
    }
}
