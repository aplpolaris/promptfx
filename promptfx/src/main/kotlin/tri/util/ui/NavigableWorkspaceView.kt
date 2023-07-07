package tri.util.ui

import tornadofx.Workspace
import java.util.*

/** A view that can be added to a workspace. */
interface NavigableWorkspaceView {

    val category: String
    val name: String
//    val description: String
//    val icon: String

    /** Add the view to the workspace. */
    fun dock(workspace: Workspace)

    companion object {
        val viewPlugins: List<NavigableWorkspaceView> by lazy {
            ServiceLoader.load(NavigableWorkspaceView::class.java).toList().also {
                it.forEach {
                    println("Loaded NavigableWorkspaceView: ${it.category} - ${it.name}")
                }
            }
        }
    }
}

