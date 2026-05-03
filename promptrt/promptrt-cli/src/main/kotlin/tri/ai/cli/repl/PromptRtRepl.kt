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

import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.TerminalBuilder
import tri.ai.cli.config.PromptRtConfig
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
            is ReplCommand.Memory  -> printInfo("[stub] /memory not yet wired")
            is ReplCommand.Rag     -> printInfo("[stub] /rag not yet wired")
            is ReplCommand.Tools   -> printInfo("[stub] /tools not yet wired")
            is ReplCommand.Batch   -> printInfo("[stub] /batch not yet wired")
            is ReplCommand.Chat    -> printInfo("[chat stub] ${cmd.text}")
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

    private fun printStatus() {
        printInfo("""
            Mode:        ${state.activeMode.name}
            Model:       ${state.effectiveModel}
            Memory:      ${state.memoryEnabled}
            RAG:         ${state.ragEnabled}${if (state.ragPath != null) " (${state.ragPath})" else ""}
            Tools:       ${state.toolsEnabled}
            Stream:      ${state.streamEnabled}
            JSON mode:   ${state.jsonMode}
            Temperature: ${state.temperature}
            Top-P:       ${state.topP ?: "default"}
            Seed:        ${state.seed ?: "none"}
            System:      ${state.systemPrompt ?: "none"}
            History:     ${state.history.size} messages
        """.trimIndent())
    }

    internal fun printResponse(text: String) = println("$ANSI_LIGHTBLUE$text$ANSI_RESET")
    internal fun printInfo(text: String)     = println("$ANSI_GRAY$text$ANSI_RESET")
    internal fun printError(text: String)    = println("${ANSI_RED}ERROR: $text$ANSI_RESET")
}
