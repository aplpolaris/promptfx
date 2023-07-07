package tri.util.ui

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tri.util.ui.NavigableWorkspaceView

class NavigableWorkspaceViewTest {
    @Test
    fun testPlugins() {
        NavigableWorkspaceView.viewPlugins.forEach {
            println(it)
        }
        assertTrue(NavigableWorkspaceView.viewPlugins.isNotEmpty())
    }
}