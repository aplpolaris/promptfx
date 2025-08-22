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
import org.junit.jupiter.api.Assertions.*

class ConfigurationViewTest {
    
    @Test
    fun testConfigurationPluginSetup() {
        val plugin = ConfigurationPlugin()
        
        assertEquals("Settings", plugin.category)
        assertEquals("Configuration", plugin.name)
        assertEquals(ConfigurationView::class, plugin.type)
    }
    
    @Test
    fun testConfigCategoryEnum() {
        // Test that all required categories are defined
        val categories = ConfigCategory.values()
        
        assertTrue(categories.any { it.displayName == "Runtime" })
        assertTrue(categories.any { it.displayName == "APIs/Models" })
        assertTrue(categories.any { it.displayName == "Views" })
        assertTrue(categories.any { it.displayName == "Configuration Files" })
        assertTrue(categories.any { it.displayName == "PromptFx Config" })
        assertTrue(categories.any { it.displayName == "Starship Config" })
        
        // Test that each category has a display name and icon
        categories.forEach { category ->
            assertNotNull(category.displayName)
            assertFalse(category.displayName.isBlank())
            assertNotNull(category.icon)
        }
    }
}