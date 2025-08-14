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
package tri.promptfx.ui

import tornadofx.*
import tri.util.ui.NavigableWorkspaceView
import tri.util.ui.WorkspaceViewAffordance

/** View that is configured entirely at runtime. */
class NavigableWorkspaceViewRuntime(val config: RuntimePromptViewConfig) : NavigableWorkspaceView {
    override val category: String
        get() = config.category
    override val name: String
        get() = config.title
    override val affordances: WorkspaceViewAffordance
        get() = WorkspaceViewAffordance.INPUT_ONLY // TODO - should this be added into runtime configs??
    val view by lazy {
        viewCache.getOrPut(config) { RuntimePromptView(config) }
    }

    override fun dock(workspace: Workspace) {
        view.scope.workspace = workspace
        workspace.dock(view)
    }

    companion object {
        private val viewCache = mutableMapOf<RuntimePromptViewConfig, RuntimePromptView>()
    }
}

/** View that is configured entirely at runtime. */
class NavigableWorkspaceViewRuntimeMcp(val config: RuntimePromptViewConfigMcp) : NavigableWorkspaceView {
    override val category: String
        get() = config.category
    override val name: String
        get() = config.prompt.title ?: config.prompt.name
    override val affordances: WorkspaceViewAffordance
        get() = WorkspaceViewAffordance.INPUT_ONLY // TODO - should this be added into runtime configs??
    val view by lazy {
        viewCache.getOrPut(config) { RuntimePromptViewMcp(config) }
    }

    override fun dock(workspace: Workspace) {
        view.scope.workspace = workspace
        workspace.dock(view)
    }

    companion object {
        private val viewCache = mutableMapOf<RuntimePromptViewConfigMcp, RuntimePromptViewMcp>()
    }
}
