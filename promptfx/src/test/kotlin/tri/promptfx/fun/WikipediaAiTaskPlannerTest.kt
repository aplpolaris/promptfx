package tri.promptfx.`fun`

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tri.ai.pips.AiPipelineExecutor
import tri.ai.pips.PrintMonitor
import tri.ai.openai.OpenAiCompletion
import tri.ai.openai.OpenAiModelIndex

class WikipediaAiTaskPlannerTest {

    val engine = OpenAiCompletion(OpenAiModelIndex.GPT35_TURBO_INSTRUCT)

    @Test
    fun testPlanner() {
        val tasks = WikipediaAiTaskPlanner(engine, null, "How big is Texas?").plan()
        assertEquals(4, tasks.size)
        assertEquals("wikipedia-page-guess", tasks[0].id)
    }

    @Test
    @Disabled("Requires apikey")
    fun testExecute() = runTest {
        val tasks = WikipediaAiTaskPlanner(engine, null, "How big is Texas?").plan()
        val result = AiPipelineExecutor.execute(tasks, PrintMonitor())
        assertEquals(4, result.interimResults.size)
        println(result.finalResult)
    }

}