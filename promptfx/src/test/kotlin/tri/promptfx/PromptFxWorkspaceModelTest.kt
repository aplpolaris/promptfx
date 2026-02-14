/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.promptfx.ui.NavigableWorkspaceViewRuntime

class PromptFxWorkspaceModelTest {

    private val BUILT_IN_CATEGORIES = setOf("API", "Agents", "Documents", "Fun", "Multimodal", "Prompts", "Settings", "Text")

    @Test
    fun testCustomCategoryHandling() {
        // Test that runtime views are properly categorized
        val model = PromptFxWorkspaceModel.instance
        
        // Verify Custom tab exists if there are runtime views with non-built-in categories
        val customTab = model.viewGroups.find { it.category == "Custom" }
        
        // If runtime views exist with custom categories, Custom tab should exist
        val customCategoryViews = RuntimePromptViewConfigs.viewConfigs.filter { view ->
            view.viewGroup !in BUILT_IN_CATEGORIES
        }
        
        if (customCategoryViews.isNotEmpty()) {
            assertNotNull(customTab, "Custom tab should exist when there are views with non-built-in categories")
            // Custom tab can now contain both runtime views and plugin views with custom categories
            assertTrue(customTab!!.views.all { 
                it is NavigableWorkspaceViewRuntime || !BUILT_IN_CATEGORIES.contains(it.category) 
            }, "All views in Custom tab should be runtime views or plugin views with custom categories")
        }
        
        // Verify built-in categories contain the appropriate views
        model.viewGroups.filter { it.category != "Custom" }.forEach { group ->
            assertTrue(group.category in BUILT_IN_CATEGORIES,
                "Built-in category '${group.category}' should be a recognized category")
        }
    }
    
    @Test
    fun testRuntimeViewsInBuiltInCategories() {
        // Test that runtime views with built-in categories are properly placed
        val model = PromptFxWorkspaceModel.instance
        
        // Find runtime views that match built-in categories
        val runtimeViewsInBuiltInCategories = RuntimePromptViewConfigs.viewConfigs.filter { view ->
            view.viewGroup in BUILT_IN_CATEGORIES
        }
        
        // Verify these runtime views are in the appropriate built-in category tabs
        runtimeViewsInBuiltInCategories.forEach { runtimeView ->
            val category = runtimeView.viewGroup
            val categoryTab = model.viewGroups.find { it.category == category }
            assertNotNull(categoryTab, "Category tab '$category' should exist for runtime view")
            
            val runtimeViewName = runtimeView.viewId
            val hasRuntimeView = categoryTab!!.views.any { it.name == runtimeViewName }
            assertTrue(hasRuntimeView, "Runtime view '$runtimeViewName' should be in category '$category'")
        }
    }
}
