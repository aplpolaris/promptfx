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
package tri.promptfx.research

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import tri.util.ui.NavigableWorkspaceView

class ResearchPluginIntegrationTest {

    @Test
    fun `test ResearchViewPlugin is registered as service`() {
        val plugins = NavigableWorkspaceView.viewPlugins
        val researchPlugin = plugins.find { it is ResearchViewPlugin }
        
        assertNotNull(researchPlugin, "ResearchViewPlugin should be registered as a service")
        assertTrue(researchPlugin is ResearchViewPlugin, "Found plugin should be ResearchViewPlugin")
        
        val plugin = researchPlugin as ResearchViewPlugin
        assertEquals("Research", plugin.category)
        assertEquals("Research View", plugin.name)
    }
    
    @Test
    fun `test all expected plugins are loaded`() {
        val plugins = NavigableWorkspaceView.viewPlugins
        val pluginNames = plugins.map { it.name }
        
        // Check that our plugin is included along with other expected plugins
        assertTrue(pluginNames.contains("Research View"), "Research View should be in plugin list")
        assertTrue(pluginNames.contains("Document Q&A"), "Document Q&A should be in plugin list")
        
        println("Loaded plugins: ${pluginNames.joinToString(", ")}")
        
        // Verify we have a reasonable number of plugins (should be more than just ours)
        assertTrue(plugins.size > 5, "Should have loaded multiple plugins, found ${plugins.size}")
    }
}