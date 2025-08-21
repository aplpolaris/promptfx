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
        /** The singleton instance of the workspace model. */
        val instance: PromptFxWorkspaceModel by lazy {
            val categories = NavigableWorkspaceView.viewPlugins.map { it.category }.toSet() +
                    RuntimePromptViewConfigs.views.values.map { it.prompt.category ?: "Uncategorized" }.toSet()

            val viewsById = NavigableWorkspaceView.viewPlugins.associateBy { it.name }
            val viewsRuntimeById = RuntimePromptViewConfigs.views.values.associateBy { it.prompt.title ?: it.prompt.name ?: it.prompt.id }
            val allViewsById = mutableMapOf<String, NavigableWorkspaceView>().apply {
                putAll(viewsById)
                putAll(viewsRuntimeById.mapValues { NavigableWorkspaceViewRuntime(it.value) })
            }

            info<PromptFxWorkspaceModel>("Loading view configuration...")
            val viewGroups = categories.associateWith {
                val viewsInCategory = allViewsById.values.filter { view -> view.category == it }
                    .sortedBy { it.name }

                //region LOGGING
                val viewLoggingInfo = viewsInCategory.map { view ->
                    val color = when (view.name) {
                        in viewsById -> ANSI_YELLOW
                        in viewsRuntimeById -> ANSI_GREEN
                        else -> throw IllegalStateException("Impossible.")
                    }
                    val viewExistsMoreThanOnce = listOfNotNull(viewsById[view.name], viewsRuntimeById[view.name]).size > 1
                    if (viewExistsMoreThanOnce)
                        "$color${view.name}*$ANSI_RESET"
                    else
                        "$color${view.name}$ANSI_RESET"
                }
                info<NavigableWorkspaceView>("  - $it: ${viewLoggingInfo.joinToString()}")
                //endregion

                ViewGroupModel(it, groupIcon(it), viewsInCategory)
            }

            PromptFxWorkspaceModel(viewGroups.values.toList())
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
