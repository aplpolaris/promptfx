/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
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
package tri.promptfx.agent

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.util.ui.NavigableWorkspaceView
import java.util.*

class AgentChatPluginTest {

    @Test
    fun testPluginIsDiscoverable() {
        val plugins = ServiceLoader.load(NavigableWorkspaceView::class.java).toList()
        val agentChatPlugin = plugins.find { it is AgentChatPlugin }
        
        assertNotNull(agentChatPlugin, "AgentChatPlugin should be discoverable via ServiceLoader")
        
        val plugin = agentChatPlugin as AgentChatPlugin
        assertEquals("Agents", plugin.category)
        assertEquals("Agent Chat", plugin.name)
        assertTrue(plugin.affordances.acceptsInput)
    }

    @Test
    fun testPluginCanCreateView() {
        val plugin = AgentChatPlugin()
        assertNotNull(plugin.type)
        assertEquals(AgentChatView::class.java, plugin.type.java)
    }
}