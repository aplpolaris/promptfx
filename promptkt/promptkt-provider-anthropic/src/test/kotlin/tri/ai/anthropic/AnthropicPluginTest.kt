/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.anthropic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class AnthropicPluginTest {

    val plugin = AnthropicAiPlugin()

    @Test
    fun testModelSource() {
        assertEquals("Anthropic", plugin.modelSource())
    }

    @Test
    fun testModelInfoNotEmpty() {
        val info = AnthropicModelIndex.chatModels()
        assertNotNull(info)
        assert(info.isNotEmpty()) { "Expected non-empty model list" }
    }

    @Test
    @Tag("anthropic")
    fun testModels() {
        println(plugin.modelInfo())
    }
}
