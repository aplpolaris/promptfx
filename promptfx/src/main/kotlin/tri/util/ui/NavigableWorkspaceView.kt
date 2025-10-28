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
package tri.util.ui

import tornadofx.Workspace
import tri.promptfx.PluginInfo
import tri.promptfx.PromptFxPlugins

/** A view that can be added to a workspace. */
interface NavigableWorkspaceView {

    val category: String
    val name: String
    val affordances: WorkspaceViewAffordance

    /** Add the view to the workspace. */
    fun dock(workspace: Workspace)

    companion object {
        // load view plugins from classpath and from config/plugins/ folder
        val viewPlugins: List<NavigableWorkspaceView> by lazy {
            // Load all plugins (both built-in and external) for backward compatibility
            allViewPluginsWithSource.map { it.plugin }
        }
        
        // load all plugins with source information
        val allViewPluginsWithSource: List<PluginInfo<NavigableWorkspaceView>> by lazy {
            PromptFxPlugins.loadAllPluginsWithSource(NavigableWorkspaceView::class.java)
        }
    }
}

/** Information about type of affordances for the view's primary logic. */
data class WorkspaceViewAffordance(
    /** Accepts an input document collection. */
    var acceptsCollection: Boolean = false,
    /** Accepts an input string. */
    var acceptsInput: Boolean = false,
    /** Produces an output string. */
    var producesOutput: Boolean = false
) {
    companion object {
        val NONE = WorkspaceViewAffordance()
        val INPUT_ONLY = WorkspaceViewAffordance(acceptsInput = true)
        val COLLECTION_ONLY = WorkspaceViewAffordance(acceptsCollection = true)
        val INPUT_AND_COLLECTION = WorkspaceViewAffordance(acceptsInput = true, acceptsCollection = true)
    }
}
