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

import org.jline.reader.Highlighter
import org.jline.reader.LineReader
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle

private val KNOWN_COMMANDS = setOf(
    "/mode", "/model", "/provider", "/memory", "/rag", "/tools",
    "/json", "/temp", "/topp", "/system",
    "/batch", "/status", "/models", "/providers", "/reset", "/help", "/quit"
)

class ReplHighlighter : Highlighter {
    override fun highlight(reader: LineReader, buffer: String): AttributedString {
        val builder = AttributedStringBuilder()
        if (!buffer.startsWith("/")) {
            builder.append(buffer, AttributedStyle.DEFAULT)
            return builder.toAttributedString()
        }
        val parts = buffer.split("\\s+".toRegex(), limit = 2)
        val cmd = parts[0].lowercase()
        val cmdStyle = if (cmd in KNOWN_COMMANDS)
            AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN)
        else
            AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)
        builder.append(parts[0], cmdStyle)
        if (parts.size > 1) {
            builder.append(" ", AttributedStyle.DEFAULT)
            builder.append(parts[1], AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE))
        }
        return builder.toAttributedString()
    }

    override fun setErrorPattern(errorPattern: java.util.regex.Pattern?) {}
    override fun setErrorIndex(errorIndex: Int) {}
}
