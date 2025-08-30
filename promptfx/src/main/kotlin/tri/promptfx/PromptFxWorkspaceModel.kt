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
import tri.util.ANSI_CYAN
import tri.util.ANSI_GREEN
import tri.util.ANSI_RED
import tri.util.ANSI_RESET
import tri.util.ANSI_YELLOW
import tri.util.info
import tri.util.ui.NavigableWorkspaceView
import tri.util.ui.fireOrange
import tri.util.ui.graphic

/** Model of the view content within [PromptFx] (views, groups, etc.). */
class PromptFxWorkspaceModel(
    val viewGroups: List<ViewGroupModel> = mutableListOf()
) {
    companion object {
        /** Built-in categories that should have their own tabs. */
        private val BUILT_IN_CATEGORIES = setOf("API", "Prompts", "Text", "Documents", "Multimodal", "Agents", "Fun", "Settings")
        
        /** The singleton instance of the workspace model. */
        var instance: PromptFxWorkspaceModel = loadWorkspaceModel()
            private set

        /** Reload the workspace model to pick up new runtime views. */
        fun reload() {
            instance = loadWorkspaceModel()
        }

        private fun loadWorkspaceModel(): PromptFxWorkspaceModel {
            info<PromptFxWorkspaceModel>("Loading view configuration... [${ANSI_YELLOW}plugins${ANSI_RESET}] [${ANSI_GREEN}built-in${ANSI_RESET}] [${ANSI_CYAN}runtime${ANSI_RESET}] [${ANSI_RED}user-provided${ANSI_RESET}] [${ANSI_CYAN}* overridden *${ANSI_RESET}]")

            val viewIndex = RuntimePromptViewConfigs.viewIndex
            val categories = viewIndex.values.map { it.viewGroup }.distinct()
            val customCategories = categories - BUILT_IN_CATEGORIES

            // Log summary counts
            val pluginViews = RuntimePromptViewConfigs.viewConfigs.filter { it.source == RuntimeViewSource.VIEW_PLUGIN }
            val builtInPluginViews = RuntimePromptViewConfigs.viewConfigs.filter { it.source == RuntimeViewSource.VIEW_PLUGIN_BUILTIN }
            val builtInViews = RuntimePromptViewConfigs.viewConfigs.filter { it.source == RuntimeViewSource.BUILT_IN_CONFIG }
            val runtimeViews = RuntimePromptViewConfigs.viewConfigs.filter { it.source == RuntimeViewSource.RUNTIME_CONFIG }
            info<PromptFxWorkspaceModel>("Found ${pluginViews.size} external plugin views, ${builtInPluginViews.size} built-in plugin views, ${builtInViews.size} built-in config views, ${runtimeViews.size} runtime config views")

            // log listing of all views found, by built-in categories first then custom categories
            fun SourcedViewConfig.viewLog(): String {
                val allSources = RuntimePromptViewConfigs.viewConfigs.filter { it.viewId == viewId }.map { it.source }
                return when {
                    allSources.size > 1 -> "$ANSI_CYAN* $viewId *$ANSI_RESET"
                    source == RuntimeViewSource.VIEW_PLUGIN -> "$ANSI_YELLOW$viewId$ANSI_RESET"
                    source == RuntimeViewSource.VIEW_PLUGIN_BUILTIN -> "$ANSI_GREEN$viewId$ANSI_RESET"
                    source == RuntimeViewSource.BUILT_IN_CONFIG -> "$ANSI_GREEN$viewId$ANSI_RESET"
                    source == RuntimeViewSource.RUNTIME_CONFIG -> "$ANSI_CYAN$viewId$ANSI_RESET"
                    source == RuntimeViewSource.USER_PROVIDED -> "$ANSI_RED$viewId$ANSI_RESET" // just in case we add user-provided source later
                    else -> throw IllegalStateException("Impossible.")
                }
            }
            (BUILT_IN_CATEGORIES + customCategories).forEach { cat ->
                val viewsInCategory = viewIndex.values.filter { it.viewGroup == cat }.map { it.viewLog() }
                val catStr = if (cat in BUILT_IN_CATEGORIES) cat else "$ANSI_GREEN$cat$ANSI_RESET"
                when {
                    viewsInCategory.isEmpty() -> info<NavigableWorkspaceView>("  - $catStr: (no views)")
                    else -> info<NavigableWorkspaceView>("  - $catStr: ${viewsInCategory.joinToString()}")
                }
            }

            // construct view groups
            val builtInViewGroups = BUILT_IN_CATEGORIES.map {
                ViewGroupModel(it, groupIcon(it),
                    viewIndex.values.filter { v -> v.viewGroup == it }
                        .map { it.view ?: NavigableWorkspaceViewRuntime(it.config!!) }
                        .sortedBy { it.name }
                )
            }
            val customViewGroup = ViewGroupModel("Custom", groupIcon("Custom"),
                viewIndex.values.filter { it.viewGroup !in BUILT_IN_CATEGORIES }
                    .map { it.view ?: NavigableWorkspaceViewRuntime(it.config!!) }
                    .sortedBy { "${it.category}/${it.name}" }
            )

            return PromptFxWorkspaceModel(builtInViewGroups + customViewGroup)
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
