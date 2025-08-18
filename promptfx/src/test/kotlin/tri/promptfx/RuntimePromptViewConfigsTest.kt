package tri.promptfx

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RuntimePromptViewConfigsTest {

    @Test
    fun testLoadViews() {
        val views = RuntimePromptViewConfigs.views
        views.values.groupBy { it.category }.toSortedMap().forEach { (category, viewList) ->
            println("$category/")
            viewList.sortedBy { it.title }.forEach {
                println("  " + it.title.padEnd(30) + it.promptConfig().id)
            }
        }
        assertTrue(views.isNotEmpty())
    }

    @Test
    fun testLoadMcpViews() {
        val views = RuntimePromptViewConfigs.mcpViews
        views.values.groupBy { it.category }.toSortedMap().forEach { (category, viewList) ->
            println("$category/")
            viewList.sortedBy { it.prompt.title!! }.forEach {
                println("  " + it.prompt.title!!.padEnd(30) + it.prompt.id)
            }
        }
        assertTrue(views.isNotEmpty())
    }

}