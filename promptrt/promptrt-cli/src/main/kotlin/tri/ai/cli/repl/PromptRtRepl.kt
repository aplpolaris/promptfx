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
package tri.ai.cli.repl

import kotlinx.coroutines.runBlocking
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.TerminalBuilder
import tri.ai.cli.config.PromptRtConfig
import tri.ai.core.AiChatEngine
import tri.ai.core.AiModelProvider
import tri.ai.core.MChatRole
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.TextChatMessage
import tri.ai.core.allChatEngines
import tri.ai.core.chatEngine
import tri.ai.core.agent.AgentEventPrinter
import tri.ai.memory.HelperPersona
import tri.ai.pips.ExecEvent
import tri.ai.memory.MemoryItem
import tri.ai.prompt.trace.AiOutput
import tri.util.ANSI_CYAN
import tri.util.ANSI_GRAY
import tri.util.ANSI_LIGHTBLUE
import tri.util.ANSI_RED
import tri.util.ANSI_RESET
import java.io.File

class PromptRtRepl(private val config: PromptRtConfig) {

    private val state = SessionState.fromConfig(config)

    fun start() {
        val terminal = TerminalBuilder.builder().system(true).build()
        val history = DefaultHistory()
        val reader = LineReaderBuilder.builder()
            .terminal(terminal)
            .completer(ReplCompleter(config))
            .highlighter(ReplHighlighter())
            .history(history)
            .variable(org.jline.reader.LineReader.HISTORY_FILE,
                File(System.getProperty("user.home"), ".promptrt/history").absolutePath)
            .build()

        printInfo("promptrt — type /help for commands, /quit to exit")
        printInfo("Mode: ${state.activeMode.name}  Model: ${state.effectiveModel}")

        while (true) {
            val line = try {
                reader.readLine("> ")
            } catch (e: UserInterruptException) {
                continue
            } catch (e: EndOfFileException) {
                break
            }

            if (line.isBlank()) continue
            dispatch(CommandParser.parse(line))
        }
    }

    internal fun dispatch(cmd: ReplCommand) {
        when (cmd) {
            is ReplCommand.Quit    -> { printInfo("Goodbye!"); kotlin.system.exitProcess(0) }
            is ReplCommand.Help    -> printHelp()
            is ReplCommand.Status  -> printStatus()
            is ReplCommand.Models  -> printModels()
            is ReplCommand.Providers -> printProviders()
            is ReplCommand.Reset   -> { state.reset(config); printInfo("reset → mode: ${state.activeMode.name}  model: ${state.effectiveModel}") }
            is ReplCommand.Unknown -> printError(cmd.input)
            is ReplCommand.Mode    -> {
                val preset = config.resolveMode(cmd.name)
                state.switchMode(preset)
                printInfo("mode: ${state.activeMode.name}  model: ${state.effectiveModel}  (history cleared)")
            }
            is ReplCommand.Model   -> { state.applyModelOverride(cmd.id); printInfo("model: ${cmd.id}") }
            is ReplCommand.Provider -> printInfo("[stub] /provider not yet wired")
            is ReplCommand.Temp    -> { state.temperature = cmd.value; printInfo("temperature: ${cmd.value}") }
            is ReplCommand.TopP    -> { state.topP = cmd.value; printInfo("top-p: ${cmd.value}") }
            is ReplCommand.Seed    -> { state.seed = cmd.value; printInfo("seed: ${cmd.value}") }
            is ReplCommand.SystemPrompt -> { state.systemPrompt = cmd.text; printInfo("system prompt updated") }
            is ReplCommand.Stream  -> { state.streamEnabled = cmd.on; printInfo("stream: ${cmd.on}") }
            is ReplCommand.JsonMode -> { state.jsonMode = cmd.on; printInfo("json: ${cmd.on}") }
            is ReplCommand.Memory -> {
                state.memoryEnabled = cmd.on
                if (cmd.on) {
                    try { state.getOrCreateMemory() }
                    catch (e: Exception) { printError("Memory init failed: ${e.message}"); state.memoryEnabled = false; return }
                } else {
                    state.botMemory = null
                }
                printInfo("memory: ${cmd.on}")
            }
            is ReplCommand.Rag -> {
                if (!cmd.on) {
                    state.ragDriver?.close()
                    state.ragDriver = null
                    state.ragEnabled = false
                    printInfo("rag: off")
                } else {
                    if (cmd.path != null) state.ragPath = cmd.path
                    if (state.ragPath == null) {
                        printError("/rag requires a path — use /rag <path> or /rag on after setting a path")
                        return
                    }
                    state.ragEnabled = true
                    printInfo("rag: on (${state.ragPath})")
                }
            }
            is ReplCommand.Tools   -> {
                state.toolsEnabled = cmd.on
                if (!cmd.on) state.agentSession = null
                printInfo("tools: ${cmd.on}")
            }
            is ReplCommand.Batch   -> {
                val file = java.io.File(cmd.path)
                if (!file.exists()) {
                    printError("File not found: ${cmd.path}")
                    return
                }
                val outFile = java.io.File(file.parent, file.nameWithoutExtension + "-output.json")
                printInfo("Running batch: ${file.name} → ${outFile.name}")
                try {
                    val path = tri.ai.cli.batch.BatchRunner.execute(file, outFile)
                    printInfo("Done. Output: $path")
                } catch (e: Exception) {
                    printError("Batch failed: ${e.message}")
                }
            }
            is ReplCommand.Chat    -> handleChat(cmd.text)
        }
    }

    private fun printModels() {
        val engines = AiModelProvider.allChatEngines()
        val embed = AiModelProvider.embeddingModels()
        println("${ANSI_CYAN}─── chat models (${engines.size}) ────────────────────${ANSI_RESET}")
        engines.forEach { e ->
            val tag = if (e is AiChatEngine.Multimodal) " ${ANSI_GRAY}[multimodal]${ANSI_RESET}" else ""
            println("  ${e.modelId}$tag")
        }
        if (embed.isNotEmpty()) {
            println("${ANSI_CYAN}─── embedding models (${embed.size}) ──────────────${ANSI_RESET}")
            embed.forEach { println("  ${it.modelId}") }
        }
        println("${ANSI_CYAN}────────────────────────────────────────${ANSI_RESET}")
    }

    private fun printProviders() {
        val plugins = AiModelProvider.orderedPlugins
        println("${ANSI_CYAN}─── providers (${plugins.size}) ─────────────────────${ANSI_RESET}")
        if (plugins.isEmpty()) {
            printError("No providers loaded. Check API key environment variables.")
        } else {
            plugins.forEach { p -> println("  ${p.javaClass.simpleName}") }
            println()
            println("  ${ANSI_GRAY}Total chat models: ${AiModelProvider.allChatEngines().size}${ANSI_RESET}")
            println("  ${ANSI_GRAY}Total embedding models: ${AiModelProvider.embeddingModels().size}${ANSI_RESET}")
        }
        println("${ANSI_CYAN}────────────────────────────────────────${ANSI_RESET}")
    }

    private fun printHelp() {
        printInfo("""
            Commands:
              /mode <name>        switch mode (${config.allModeNames.joinToString(", ")})
              /model <id>         override model for session
              /provider <name>    switch provider
              /memory <on|off>    toggle memory
              /rag <on|off|path>  toggle RAG
              /tools <on|off>     toggle tool use
              /stream <on|off>    toggle streaming
              /json <on|off>      toggle JSON output mode
              /system <text>      set system prompt
              /temp <n>           set temperature
              /topp <n>           set top-p
              /seed <n>           set sampling seed
              /batch <file>       run a batch job
              /status             show current session config
              /models             list available models
              /providers          list loaded providers
              /reset              restore default mode
              /help               show this help
              /quit               exit
        """.trimIndent())
    }

    private fun statusLine(label: String, value: String) =
        println("$ANSI_CYAN  %-12s$ANSI_RESET $value".format(label))

    private fun printStatus() {
        println("${ANSI_CYAN}─── session status ───────────────────${ANSI_RESET}")
        statusLine("mode:",     state.activeMode.name)
        statusLine("model:",    state.effectiveModel)
        statusLine("memory:",   state.memoryEnabled.toString())
        statusLine("rag:",      if (state.ragEnabled) "on${if (state.ragPath != null) " (${state.ragPath})" else ""}" else "off")
        statusLine("tools:",    state.toolsEnabled.toString())
        statusLine("stream:",   state.streamEnabled.toString())
        statusLine("json:",     state.jsonMode.toString())
        statusLine("temp:",     state.temperature.toString())
        statusLine("top-p:",    state.topP?.toString() ?: "default")
        statusLine("seed:",     state.seed?.toString() ?: "none")
        statusLine("system:",   state.systemPrompt ?: "none")
        statusLine("history:",  "${state.history.size} messages")
        println("${ANSI_CYAN}──────────────────────────────────────${ANSI_RESET}")
    }

    private fun handleChat(userInput: String) {
        if (state.memoryEnabled) { handleMemoryChat(userInput); return }
        if (state.ragEnabled)    { handleRagChat(userInput); return }
        if (state.toolsEnabled)  { handleAgentChat(userInput); return }

        val model = try {
            AiModelProvider.chatEngine(state.effectiveModel).asTextChat()
        } catch (e: NoSuchElementException) {
            printError("Model '${state.effectiveModel}' not found. Available: ${
                AiModelProvider.allChatEngines().map { it.modelId }.joinToString()
            }")
            return
        }

        val messages = buildList {
            if (state.systemPrompt != null)
                add(TextChatMessage(MChatRole.System, state.systemPrompt!!))
            addAll(state.history)
            add(TextChatMessage(MChatRole.User, userInput))
        }

        state.history.add(TextChatMessage(MChatRole.User, userInput))

        val response = runBlocking {
            try {
                model.chat(messages)
            } catch (e: Exception) {
                printError("Model error: ${e.message}")
                null
            }
        } ?: return

        val message = (response.firstValue as AiOutput.ChatMessage).message
        printInfo("[${state.effectiveModel}]")
        printResponse(message.content ?: "")
        state.history.add(TextChatMessage(MChatRole.Assistant, message.content ?: ""))

        // Trim history to prevent unbounded growth
        while (state.history.size > 40) state.history.removeAt(0)
    }

    private fun handleMemoryChat(userInput: String) {
        val memory = try {
            state.getOrCreateMemory()
        } catch (e: Exception) {
            printError("Memory unavailable: ${e.message}")
            return
        }
        val model = try {
            AiModelProvider.chatEngine(state.effectiveModel).asTextChat()
        } catch (e: NoSuchElementException) {
            printError("Model '${state.effectiveModel}' not found.")
            return
        }
        runBlocking {
            try {
                val userItem = MemoryItem(MChatRole.User, userInput)
                memory.addChat(userItem)
                val contextHistory = memory.buildContextualConversationHistory(userItem)
                    .map { it.toChatMessage() }
                val sysMsg = listOf(TextChatMessage(MChatRole.System, memory.persona.getSystemMessage()))
                val response = model.chat(sysMsg + contextHistory).firstValue
                val msg = (response as AiOutput.ChatMessage).message
                memory.addChat(MemoryItem(msg))
                memory.saveMemory(interimSave = true)
                printInfo("[${state.effectiveModel}]")
                printResponse(msg.content ?: "")
            } catch (e: Exception) {
                printError("Memory chat error: ${e.message}")
            }
        }
    }

    private fun handleRagChat(userInput: String) {
        val driver = try {
            state.getOrCreateRagDriver()
        } catch (e: Exception) {
            printError("RAG init failed: ${e.message}")
            return
        }
        if (driver == null) {
            printError("RAG enabled but no path set — use /rag <path>")
            return
        }
        runBlocking {
            try {
                val result = driver.answerQuestion(userInput)
                printInfo("[${state.effectiveModel}]")
                printResponse(result.finalResult.toString())
            } catch (e: Exception) {
                printError("RAG error: ${e.message}")
            }
        }
    }

    private fun handleAgentChat(userInput: String) {
        val session = state.getOrCreateAgentSession()
        runBlocking {
            try {
                val message = MultimodalChatMessage.text(MChatRole.User, userInput)
                val op = state.agentApi.sendMessage(session, message)
                op.events.collect { event ->
                    when (event) {
                        is ExecEvent.UsingTool ->
                            printInfo("$ANSI_GRAY⚙ [tool] ${event.toolName}: ${event.input}$ANSI_RESET")
                        is ExecEvent.ToolResult ->
                            printInfo("$ANSI_GRAY  → [result] ${event.toolName}: ${event.result}$ANSI_RESET")
                        is ExecEvent.Reasoning ->
                            printInfo("$ANSI_GRAY  ∴ [reasoning] ${event.reasoning}$ANSI_RESET")
                        is ExecEvent.Response -> {
                            val responseText = event.response.message.content?.firstOrNull()?.text ?: "[No response]"
                            printInfo("[${state.effectiveModel}]")
                            printResponse(responseText)
                        }
                        is ExecEvent.Error ->
                            printError("Agent error: ${event.error.message}")
                        else -> { /* ignore task lifecycle, progress, streaming tokens, user echo */ }
                    }
                }
            } catch (e: Exception) {
                printError("Agent error: ${e.message}")
            }
        }
    }

    internal fun printResponse(text: String) = println("$ANSI_LIGHTBLUE$text$ANSI_RESET")
    internal fun printInfo(text: String)     = println("$ANSI_GRAY$text$ANSI_RESET")
    internal fun printError(text: String)    = println("${ANSI_RED}ERROR: $text$ANSI_RESET")
}
