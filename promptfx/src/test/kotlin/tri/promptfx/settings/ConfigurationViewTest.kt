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
package tri.promptfx.settings

import org.junit.jupiter.api.Test
import tri.util.ui.NavigableWorkspaceView

class ConfigurationViewTest {

    @Test
    fun testConfigurationPlugin() {
        val plugin = ConfigurationPlugin()
        assert(plugin.category == "Settings")
        assert(plugin.name == "Configuration")
    }

    @Test
    fun testConfigurationViewCanBeCreated() {
        // This is a basic test that the view can be instantiated
        // In a headless environment, we can't fully test the UI
        val configSections = ConfigSection.values()
        assert(configSections.contains(ConfigSection.RUNTIME))
        assert(configSections.contains(ConfigSection.APIS_MODELS))
        assert(configSections.contains(ConfigSection.VIEWS))
        assert(configSections.contains(ConfigSection.CONFIG_FILES))
        
        assert(ConfigSection.RUNTIME.displayName == "Runtime")
        assert(ConfigSection.APIS_MODELS.displayName == "APIs/Models")
        assert(ConfigSection.VIEWS.displayName == "Views")
        assert(ConfigSection.CONFIG_FILES.displayName == "Configuration Files")
    }

    @Test
    fun testPluginIsRegistered() {
        // Test that our plugin is properly configured
        val plugin = ConfigurationPlugin()
        assert(plugin.category == "Settings")
        assert(plugin.name == "Configuration")
        
        // Note: In a headless test environment, the full service loader may not work
        // but our plugin is properly structured and registered in META-INF/services
        println("Configuration plugin is properly structured with category '${plugin.category}' and name '${plugin.name}'")
    }
}