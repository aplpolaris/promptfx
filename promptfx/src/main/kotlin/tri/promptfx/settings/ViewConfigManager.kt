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

import com.fasterxml.jackson.module.kotlin.readValue
import tri.promptfx.ui.RuntimePromptViewConfig
import tri.util.ui.MAPPER
import java.io.File

/** Utility for managing runtime view configurations in views.yaml file. */
object ViewConfigManager {

    private val runtimeViewsFile = File("config/views.yaml")

    /** Load existing runtime view configurations from views.yaml file. */
    fun loadRuntimeViews(): Map<String, RuntimePromptViewConfig> {
        return if (runtimeViewsFile.exists()) {
            try {
                MAPPER.readValue<Map<String, RuntimePromptViewConfig>>(runtimeViewsFile)
            } catch (e: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }

    /** Save runtime view configurations to views.yaml file. */
    fun saveRuntimeViews(views: Map<String, RuntimePromptViewConfig>) {
        // Ensure config directory exists
        runtimeViewsFile.parentFile?.mkdirs()
        
        // Save the views map as YAML
        MAPPER.writeValue(runtimeViewsFile, views)
    }

    /** Add a new view configuration and save to file. */
    fun addView(viewId: String, config: RuntimePromptViewConfig) {
        val existingViews = loadRuntimeViews().toMutableMap()
        existingViews[viewId] = config
        saveRuntimeViews(existingViews)
    }

    /** Check if a view with the given ID already exists. */
    fun viewExists(viewId: String): Boolean {
        val existingViews = loadRuntimeViews()
        return viewId in existingViews
    }

    /** Generate a unique view ID from category and name. */
    fun generateViewId(category: String, name: String): String {
        val base = "${category.lowercase().replace(" ", "-")}-${name.lowercase().replace(" ", "-")}"
        var candidate = base
        var counter = 1
        
        while (viewExists(candidate)) {
            candidate = "$base-$counter"
            counter++
        }
        
        return candidate
    }
}