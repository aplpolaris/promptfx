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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConfigLoaderTest {

    private val minimalYaml = """
        default_mode: plain
    """.trimIndent()

    private val fullYaml = """
        default_mode: rag
        modes:
          custom:
            model: claude-3-5-sonnet
            provider: anthropic
            memory: true
        providers:
          anthropic:
            api_key_env: ANTHROPIC_API_KEY
    """.trimIndent()

    @Test
    fun `minimal config loads with defaults`() {
        val config = ConfigLoader.fromYaml(minimalYaml)
        assertEquals("plain", config.defaultMode)
        assertTrue(config.modes.isEmpty())
    }

    @Test
    fun `full config loads user modes and providers`() {
        val config = ConfigLoader.fromYaml(fullYaml)
        assertEquals("rag", config.defaultMode)
        assertNotNull(config.modes["custom"])
        assertEquals("claude-3-5-sonnet", config.modes["custom"]!!.model)
        assertEquals("ANTHROPIC_API_KEY", config.providers["anthropic"]?.apiKeyEnv)
    }

    @Test
    fun `resolveMode merges user mode onto plain`() {
        val config = ConfigLoader.fromYaml(fullYaml)
        val resolved = config.resolveMode("custom")
        assertEquals("claude-3-5-sonnet", resolved.model)
        assertEquals("anthropic", resolved.provider)
        assertTrue(resolved.memoryOn)
    }

    @Test
    fun `resolveMode returns built-in for known name`() {
        val config = ConfigLoader.fromYaml(minimalYaml)
        val resolved = config.resolveMode("agent")
        assertEquals("gpt-4o", resolved.model)
        assertTrue(resolved.toolsOn)
    }

    @Test
    fun `resolveMode returns plain for unknown name`() {
        val config = ConfigLoader.fromYaml(minimalYaml)
        val resolved = config.resolveMode("nonexistent")
        assertEquals("gpt-4o-mini", resolved.model)
    }

    @Test
    fun `load returns empty config when file missing`() {
        val config = ConfigLoader.load(java.io.File("/nonexistent/path/config.yaml"))
        assertEquals("plain", config.defaultMode)
    }
}
