package tri.util.ui

import tornadofx.UIComponent
import tornadofx.Workspace
import tornadofx.find
import kotlin.reflect.KClass

/** Partial implementation of [NavigableWorkspaceView]. */
abstract class NavigableWorkspaceViewImpl<T : UIComponent>(
    override val category: String,
    override val name: String,
    private val type: KClass<T>
) : NavigableWorkspaceView {

    override fun dock(workspace: Workspace) {
        workspace.dock(find(type))
    }

}