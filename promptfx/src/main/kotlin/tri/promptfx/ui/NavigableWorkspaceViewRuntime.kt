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