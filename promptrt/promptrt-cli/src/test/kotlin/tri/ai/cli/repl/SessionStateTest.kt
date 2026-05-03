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
import tri.ai.cli.config.BuiltInModes
import tri.ai.cli.config.PromptRtConfig
import tri.ai.core.MChatRole
import tri.ai.core.TextChatMessage
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionStateTest {

    private fun plainState() = SessionState.fromConfig(PromptRtConfig())

    @Test fun `initial state matches plain mode`() {
        val s = plainState()
        assertEquals("gpt-4o-mini", s.effectiveModel)
        assertFalse(s.memoryEnabled)
        assertFalse(s.ragEnabled)
        assertFalse(s.toolsEnabled)
    }

    @Test fun `model override does not clear history`() {
        val s = plainState()
        s.history.add(TextChatMessage(MChatRole.User, "hello"))
        s.applyModelOverride("gpt-4o")
        assertEquals("gpt-4o", s.effectiveModel)
        assertEquals(1, s.history.size)
    }

    @Test fun `mode switch clears history`() {
        val s = plainState()
        s.history.add(TextChatMessage(MChatRole.User, "hello"))
        s.switchMode(BuiltInModes.all["rag"]!!)
        assertEquals(0, s.history.size)
        assertTrue(s.ragEnabled)
    }

    @Test fun `reset restores default mode and clears overrides`() {
        val s = plainState()
        s.applyModelOverride("gpt-4o")
        s.memoryEnabled = true
        s.reset(PromptRtConfig())
        assertEquals("gpt-4o-mini", s.effectiveModel)
        assertFalse(s.memoryEnabled)
        assertNull(s.modelOverride)
    }

    @Test fun `effectiveModel prefers override over mode model`() {
        val s = plainState()
        s.applyModelOverride("claude-opus")
        assertEquals("claude-opus", s.effectiveModel)
    }

    @Test fun `mode switch also clears model override`() {
        val s = plainState()
        s.applyModelOverride("gpt-4o")
        s.switchMode(BuiltInModes.all["memory"]!!)
        assertNull(s.modelOverride)
        assertEquals("gpt-4o-mini", s.effectiveModel)  // memory mode model
    }

    @Test fun `effectiveProvider comes from mode`() {
        val s = plainState()
        assertEquals("OpenAI", s.effectiveProvider)
    }
}
