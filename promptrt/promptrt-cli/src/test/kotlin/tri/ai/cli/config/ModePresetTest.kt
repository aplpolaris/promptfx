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
package tri.ai.cli.config

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ModePresetTest {

    @Test
    fun `plain built-in has expected defaults`() {
        val plain = BuiltInModes.PLAIN
        assertEquals("gpt-4o-mini", plain.model)
        assertEquals("OpenAI", plain.provider)
        assertFalse(plain.memory!!)
        assertFalse(plain.rag!!)
        assertFalse(plain.tools!!)
    }

    @Test
    fun `mode merges onto plain, unspecified fields inherit`() {
        val partial = ModePreset(name = "custom", model = "gpt-4o")
        val resolved = partial.mergedOnto(BuiltInModes.PLAIN)
        assertEquals("gpt-4o", resolved.model)
        assertEquals("OpenAI", resolved.provider)   // inherited
        assertFalse(resolved.memory!!)              // inherited
    }

    @Test
    fun `agent built-in has memory and tools on`() {
        val agent = BuiltInModes.all["agent"]!!
        assert(agent.memoryOn)
        assert(agent.toolsOn)
    }

    @Test
    fun `resolvedModel falls back to plain when null`() {
        val partial = ModePreset(name = "test")
        assertEquals("gpt-4o-mini", partial.resolvedModel)
    }
}
