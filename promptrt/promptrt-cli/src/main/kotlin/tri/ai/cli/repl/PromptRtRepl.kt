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
import tri.ai.core.AiModelProvider
import tri.ai.core.MChatRole
import tri.ai.core.TextChatMessage
import tri.ai.memory.HelperPersona
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
            is ReplCommand.Rag     -> printInfo("[stub] /rag not yet wired")
            is ReplCommand.Tools   -> printInfo("[stub] /tools not yet wired")
            is ReplCommand.Batch   -> printInfo("[stub] /batch not yet wired")
            is ReplCommand.Chat    -> handleChat(cmd.text)
        }
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
        if (state.ragEnabled)    { printInfo("[stub] /rag not yet wired"); return }
        if (state.toolsEnabled)  { printInfo("[stub] /tools not yet wired"); return }

        val model = try {
            AiModelProvider.chatModels().first { it.modelId == state.effectiveModel }
        } catch (e: NoSuchElementException) {
            printError("Model '${state.effectiveModel}' not found. Available: ${
                AiModelProvider.chatModels().map { it.modelId }.joinToString()
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
            AiModelProvider.chatModels().first { it.modelId == state.effectiveModel }
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

    internal fun printResponse(text: String) = println("$ANSI_LIGHTBLUE$text$ANSI_RESET")
    internal fun printInfo(text: String)     = println("$ANSI_GRAY$text$ANSI_RESET")
    internal fun printError(text: String)    = println("${ANSI_RED}ERROR: $text$ANSI_RESET")
}
