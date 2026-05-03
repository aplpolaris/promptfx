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

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CommandParserTest {

    @Test fun `plain text becomes Chat`() =
        assertIs<ReplCommand.Chat>(CommandParser.parse("hello world"))

    @Test fun `slash mode parses name`() =
        assertEquals(ReplCommand.Mode("rag"), CommandParser.parse("/mode rag"))

    @Test fun `slash model parses id`() =
        assertEquals(ReplCommand.Model("gpt-4o"), CommandParser.parse("/model gpt-4o"))

    @Test fun `slash memory on`() =
        assertEquals(ReplCommand.Memory(true), CommandParser.parse("/memory on"))

    @Test fun `slash memory off`() =
        assertEquals(ReplCommand.Memory(false), CommandParser.parse("/memory off"))

    @Test fun `slash rag with path`() {
        val cmd = CommandParser.parse("/rag ~/docs/kb")
        assertIs<ReplCommand.Rag>(cmd)
        assertTrue((cmd as ReplCommand.Rag).on)
        assertEquals("~/docs/kb", cmd.path)
    }

    @Test fun `slash rag off`() =
        assertEquals(ReplCommand.Rag(false), CommandParser.parse("/rag off"))

    @Test fun `slash rag on no path`() =
        assertEquals(ReplCommand.Rag(true), CommandParser.parse("/rag on"))

    @Test fun `slash tools on`() =
        assertEquals(ReplCommand.Tools(true), CommandParser.parse("/tools on"))

    @Test fun `slash temp parses double`() =
        assertEquals(ReplCommand.Temp(0.8), CommandParser.parse("/temp 0.8"))

    @Test fun `slash seed is unknown`() =
        assertIs<ReplCommand.Unknown>(CommandParser.parse("/seed 42"))

    @Test fun `slash status`() =
        assertIs<ReplCommand.Status>(CommandParser.parse("/status"))

    @Test fun `slash quit`() =
        assertIs<ReplCommand.Quit>(CommandParser.parse("/quit"))

    @Test fun `unknown command returns Unknown`() {
        val cmd = CommandParser.parse("/frobnicate")
        assertIs<ReplCommand.Unknown>(cmd)
    }

    @Test fun `missing argument returns Unknown`() {
        val cmd = CommandParser.parse("/mode")
        assertIs<ReplCommand.Unknown>(cmd)
    }

    @Test fun `slash system captures full text`() =
        assertEquals(ReplCommand.SystemPrompt("You are a pirate."),
            CommandParser.parse("/system You are a pirate."))

    @Test fun `empty input becomes Chat`() =
        assertIs<ReplCommand.Chat>(CommandParser.parse(""))

    @Test fun `slash provider parses name`() =
        assertEquals(ReplCommand.Provider("anthropic"), CommandParser.parse("/provider anthropic"))

    @Test fun `slash models`() = assertIs<ReplCommand.Models>(CommandParser.parse("/models"))

    @Test fun `slash providers`() = assertIs<ReplCommand.Providers>(CommandParser.parse("/providers"))
}
