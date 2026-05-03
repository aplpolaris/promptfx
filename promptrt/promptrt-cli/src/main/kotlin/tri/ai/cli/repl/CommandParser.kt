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

object CommandParser {
    fun parse(input: String): ReplCommand {
        if (!input.startsWith("/")) return ReplCommand.Chat(input)

        val parts = input.trim().split("\\s+".toRegex(), limit = 2)
        val cmd = parts[0].lowercase()
        val arg = parts.getOrNull(1)?.trim()

        return when (cmd) {
            "/mode"     -> arg?.let { ReplCommand.Mode(it) }
                           ?: ReplCommand.Unknown("/mode requires a mode name")
            "/model"    -> ReplCommand.Model(arg)
            "/provider" -> ReplCommand.Provider(arg)
            "/memory"   -> parseToggle(arg) { ReplCommand.Memory(it) }
            "/tools"    -> parseToggle(arg) { ReplCommand.Tools(it) }
            "/json"     -> parseToggle(arg) { ReplCommand.JsonMode(it) }
            "/rag"      -> when (arg?.lowercase()) {
                               null  -> ReplCommand.Unknown("/rag requires on, off, or a path")
                               "off" -> ReplCommand.Rag(false)
                               "on"  -> ReplCommand.Rag(true)
                               else  -> ReplCommand.Rag(true, path = arg)
                           }
            "/temp"     -> arg?.toDoubleOrNull()?.let { ReplCommand.Temp(it) }
                           ?: ReplCommand.Unknown("/temp requires a number (e.g. /temp 0.7)")
            "/topp"     -> arg?.toDoubleOrNull()?.let { ReplCommand.TopP(it) }
                           ?: ReplCommand.Unknown("/topp requires a number (e.g. /topp 0.9)")
            "/system"   -> arg?.let { ReplCommand.SystemPrompt(it) }
                           ?: ReplCommand.Unknown("/system requires prompt text")
            "/batch"    -> arg?.let { ReplCommand.Batch(it) }
                           ?: ReplCommand.Unknown("/batch requires a file path")
            "/status"   -> ReplCommand.Status
            "/models"   -> ReplCommand.Models
            "/providers" -> ReplCommand.Providers
            "/reset"    -> ReplCommand.Reset
            "/help"     -> ReplCommand.Help
            "/quit"     -> ReplCommand.Quit
            else        -> ReplCommand.Unknown("Unknown command: $cmd — type /help for commands")
        }
    }

    private fun <T : ReplCommand> parseToggle(arg: String?, ctor: (Boolean) -> T): ReplCommand =
        when (arg?.lowercase()) {
            "on"  -> ctor(true)
            "off" -> ctor(false)
            else  -> ReplCommand.Unknown("Expected 'on' or 'off', got: $arg")
        }
}
