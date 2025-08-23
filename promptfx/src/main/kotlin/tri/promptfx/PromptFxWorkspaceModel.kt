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
package tri.promptfx

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import tri.promptfx.ui.NavigableWorkspaceViewRuntime
import tri.util.ANSI_GREEN
import tri.util.ANSI_RESET
import tri.util.ANSI_YELLOW
import tri.util.info
import tri.util.ui.NavigableWorkspaceView
import tri.util.ui.fireOrange
import tri.util.ui.forestGreen
import tri.util.ui.graphic

/** Model of the view content within [PromptFx] (views, groups, etc.). */
class PromptFxWorkspaceModel(
    val viewGroups: List<ViewGroupModel> = mutableListOf()
) {
    companion object {
        /** Built-in categories that should have their own tabs. */
        private val BUILT_IN_CATEGORIES = setOf("API", "Agents", "Documents", "Fun", "Multimodal", "Prompts", "Settings", "Text")
        
        /** The singleton instance of the workspace model. */
        val instance: PromptFxWorkspaceModel by lazy {
            val builtInCategories = NavigableWorkspaceView.viewPlugins.map { it.category }.toSet()
            
            val viewsById = NavigableWorkspaceView.viewPlugins.associateBy { it.name }
            val viewsRuntimeById = RuntimePromptViewConfigs.views.values.associateBy { it.prompt.title ?: it.prompt.name ?: it.prompt.id }
            
            // Separate runtime views into those that match built-in categories and those that don't
            val runtimeViews = viewsRuntimeById.mapValues { NavigableWorkspaceViewRuntime(it.value) }
            val runtimeViewsInBuiltInCategories = runtimeViews.values.filter { it.category in BUILT_IN_CATEGORIES }
            val runtimeViewsInCustomCategories = runtimeViews.values.filter { it.category !in BUILT_IN_CATEGORIES }

            info<PromptFxWorkspaceModel>("Loading view configuration...")
            
            // Create view groups for built-in categories (including runtime views that match)
            val builtInViewGroups = builtInCategories.associateWith { category ->
                val builtInViewsInCategory = viewsById.values.filter { it.category == category }.sortedBy { it.name }
                val runtimeViewsInCategory = runtimeViewsInBuiltInCategories.filter { it.category == category }.sortedBy { it.name }
                val allViewsInCategory = builtInViewsInCategory + runtimeViewsInCategory

                //region LOGGING
                val viewLoggingInfo = allViewsInCategory.map { view ->
                    val color = when (view.name) {
                        in viewsById -> ANSI_YELLOW
                        in runtimeViews -> ANSI_GREEN
                        else -> throw IllegalStateException("Impossible.")
                    }
                    val viewExistsMoreThanOnce = listOfNotNull(viewsById[view.name], runtimeViews[view.name]).size > 1
                    if (viewExistsMoreThanOnce)
                        "$color${view.name}*$ANSI_RESET"
                    else
                        "$color${view.name}$ANSI_RESET"
                }
                info<NavigableWorkspaceView>("  - $category: ${viewLoggingInfo.joinToString()}")
                //endregion

                ViewGroupModel(category, groupIcon(category), allViewsInCategory)
            }
            
            // Create Custom tab for runtime views with non-built-in categories
            val customViewGroups = if (runtimeViewsInCustomCategories.isNotEmpty()) {
                val customCategories = runtimeViewsInCustomCategories.map { it.category }.distinct().sorted()
                info<NavigableWorkspaceView>("  - Custom: ${customCategories.joinToString { cat -> 
                    val viewsInCat = runtimeViewsInCustomCategories.filter { it.category == cat }
                    "$ANSI_GREEN$cat (${viewsInCat.size} views)$ANSI_RESET" 
                }}")
                
                listOf(ViewGroupModel("Custom", groupIcon("Custom"), runtimeViewsInCustomCategories.sortedBy { "${it.category}/${it.name}" }))
            } else {
                emptyList()
            }

            PromptFxWorkspaceModel(builtInViewGroups.values.toList() + customViewGroups)
        }
        private fun groupIcon(category: String): FontAwesomeIconView {
            return when (category) {
                "API" -> FontAwesomeIcon.CLOUD
                "Prompts" -> FontAwesomeIcon.WRENCH
                "Documents" -> FontAwesomeIcon.FILE
                "Text" -> FontAwesomeIcon.FONT
                "Fun" -> FontAwesomeIcon.SMILE_ALT
                "Audio" -> FontAwesomeIcon.MICROPHONE
                "Vision" -> FontAwesomeIcon.IMAGE
                "Multimodal" -> FontAwesomeIcon.IMAGE
                "Integrations" -> FontAwesomeIcon.PLUG
                "Documentation" -> FontAwesomeIcon.BOOK
                "Agents" -> FontAwesomeIcon.ANDROID
                "Settings" -> FontAwesomeIcon.COG
                "Custom" -> FontAwesomeIcon.CUBES
                else -> FontAwesomeIcon.CUBES
            }.graphic.fireOrange
        }
    }
}

/** Model of a group of related views in the workspace. */
class ViewGroupModel(
    val category: String,
    val icon: FontAwesomeIconView,
    val views: List<NavigableWorkspaceView>
)
