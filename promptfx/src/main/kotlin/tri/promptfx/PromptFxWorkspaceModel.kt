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
import tri.util.*
import tri.util.ui.NavigableWorkspaceView
import tri.util.ui.graphic
import tri.util.ui.steelBlue

/** Model of the view content within [PromptFx] (views, groups, etc.). */
class PromptFxWorkspaceModel(
    val viewGroups: List<ViewGroupModel> = mutableListOf()
) {
    companion object {
        /** Built-in categories that should have their own tabs. */
        val BUILT_IN_CATEGORIES = setOf("API", "Prompts", "Text", "Documents", "Multimodal", "MCP", "Agents", "Settings")
        
        /** The singleton instance of the workspace model. */
        var instance: PromptFxWorkspaceModel = loadWorkspaceModel()
            private set

        /** Reload the workspace model to pick up new runtime views. */
        fun reload() {
            instance = loadWorkspaceModel()
        }

        private fun loadWorkspaceModel(): PromptFxWorkspaceModel {
            // do this first for cleaner logging
            val viewIndex = RuntimePromptViewConfigs.viewIndex

            info<PromptFxWorkspaceModel>("Loading view configuration...")

            val categories = viewIndex.values.map { it.viewGroup }.distinct()
            val customCategories = categories - BUILT_IN_CATEGORIES

            // log information about what we found
            fun RuntimeViewSource.ansi(item: Any) = when (this) {
                RuntimeViewSource.BUILT_IN_PLUGIN -> "$ANSI_GREEN$item$ANSI_RESET"
                RuntimeViewSource.BUILT_IN_CONFIG -> "$ANSI_YELLOW$item$ANSI_RESET"
                RuntimeViewSource.RUNTIME_PLUGIN -> "$ANSI_LIGHTGREEN$item$ANSI_RESET"
                RuntimeViewSource.RUNTIME_CONFIG -> "$ANSI_ORANGE$item$ANSI_RESET"
                RuntimeViewSource.USER_PROVIDED -> "$ANSI_RED$item$ANSI_RESET"
            }
            fun SourcedViewConfig.viewLog(): String {
                val allSources = RuntimePromptViewConfigs.viewConfigs.filter { it.viewId == viewId }.map { it.source }
                return source.ansi(viewId).let {
                    if (allSources.size > 1) "*$it*" else it
                }
            }
            (BUILT_IN_CATEGORIES + customCategories).forEach { cat ->
                val viewsInCategory = viewIndex.values.filter { it.viewGroup == cat }.map { it.viewLog() }
                val catStr = if (cat in BUILT_IN_CATEGORIES) cat else "$ANSI_GRAY$cat$ANSI_RESET"
                when {
                    viewsInCategory.isEmpty() -> info<NavigableWorkspaceView>("  - $catStr: (no views)")
                    else -> info<NavigableWorkspaceView>("  - $catStr: ${viewsInCategory.joinToString()}")
                }
            }
            val views = viewIndex.values.groupBy { it.source }.mapValues { it.value.size }
            val joinedInfo = views.entries.joinToString(", ") { "${it.value} ${it.key.ansi(it.key)} views" }
            info<PromptFxWorkspaceModel>("Found $joinedInfo")

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
                "Agents" -> FontAwesomeIcon.ANDROID
                "Documents" -> FontAwesomeIcon.FILE
                "MCP" -> FontAwesomeIcon.DASHCUBE
                "Multimodal" -> FontAwesomeIcon.IMAGE
                "Prompts" -> FontAwesomeIcon.WRENCH
                "Settings" -> FontAwesomeIcon.COG
                "Text" -> FontAwesomeIcon.FONT
                else -> FontAwesomeIcon.PUZZLE_PIECE
            }.graphic.steelBlue
        }
    }
}

/** Model of a group of related views in the workspace. */
class ViewGroupModel(
    val category: String,
    val icon: FontAwesomeIconView,
    val views: List<NavigableWorkspaceView>
)
