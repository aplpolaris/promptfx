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

import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine
import tri.ai.cli.config.PromptRtConfig
import tri.ai.core.AiModelProvider
import tri.ai.core.allChatEngines

private val SLASH_COMMANDS = listOf(
    "/mode", "/model", "/provider", "/memory", "/rag", "/tools",
    "/stream", "/json", "/temp", "/topp", "/seed", "/system",
    "/batch", "/status", "/models", "/providers", "/reset", "/help", "/quit"
)

class ReplCompleter(private val config: PromptRtConfig) : Completer {
    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        val word = line.word()
        val words = line.words()

        when {
            words.size <= 1 -> SLASH_COMMANDS
                .filter { it.startsWith(word) }
                .forEach { candidates.add(Candidate(it)) }

            words[0] == "/mode" ->
                config.allModeNames
                    .filter { it.contains(word, ignoreCase = true) }
                    .forEach { candidates.add(Candidate(it)) }

            words[0] == "/model" ->
                try {
                    AiModelProvider.allChatEngines()
                        .map { it.modelId }
                        .filter { it.contains(word, ignoreCase = true) }
                        .forEach { candidates.add(Candidate(it)) }
                } catch (_: Exception) { /* provider not available, skip */ }

            words[0] in listOf("/memory", "/tools", "/stream", "/json") ->
                listOf("on", "off")
                    .filter { it.startsWith(word) }
                    .forEach { candidates.add(Candidate(it)) }
        }
    }
}
