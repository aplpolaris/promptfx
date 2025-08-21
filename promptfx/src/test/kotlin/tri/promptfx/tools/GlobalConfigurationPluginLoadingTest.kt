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
package tri.promptfx.tools

import org.junit.jupiter.api.Test
import tri.util.ui.NavigableWorkspaceView
import java.util.*

class GlobalConfigurationPluginLoadingTest {
    @Test
    fun testServiceLoading() {
        println("Testing GlobalConfiguration plugin loading...")
        
        // Try loading with explicit error catching
        val loader = ServiceLoader.load(NavigableWorkspaceView::class.java)
        val services = mutableListOf<NavigableWorkspaceView>()
        
        for (service in loader) {
            try {
                services.add(service)
                println("  - ${service.javaClass.name}: ${service.category} - ${service.name}")
            } catch (e: Exception) {
                println("  - ERROR loading service: ${e.message}")
                e.printStackTrace()
            }
        }
        
        println("Total plugins found: ${services.size}")
        
        val globalConfig = services.find { it.javaClass.name.contains("GlobalConfiguration") }
        if (globalConfig != null) {
            println("Found GlobalConfiguration plugin: ${globalConfig.category} - ${globalConfig.name}")
        } else {
            println("GlobalConfiguration plugin not found!")
            
            // Try to manually instantiate it
            try {
                val plugin = GlobalConfigurationPlugin()
                println("Manual instantiation worked: ${plugin.category} - ${plugin.name}")
            } catch (e: Exception) {
                println("Manual instantiation failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}