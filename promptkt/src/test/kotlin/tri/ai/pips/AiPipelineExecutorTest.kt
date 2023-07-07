package tri.ai.pips

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AiPipelineExecutorTest {

    @Test
    fun testExecute() = runTest {
        val results = AiPipelineExecutor.execute(
            listOf(GoTask("pass"), FailTask("fail")),
            PrintMonitor()).results
        assertEquals(2, results.size)
        assertEquals("go", results["pass"]?.value)
        assertNotNull(results["fail"]?.error)
    }

    @Test
    fun testExecuteChain() = runTest {
        val results = AiPipelineExecutor.execute(
            listOf(GoTask("a"), GoTask("b", setOf("a")), GoTask("c", setOf("b"))),
            PrintMonitor()).results
        assertEquals(3, results.size)
        assertEquals("go", results["a"]?.value)
        assertEquals("go", results["b"]?.value)
        assertEquals("go", results["c"]?.value)
    }

    @Test
    fun testExecuteChainWithFailure() = runTest {
        val results = AiPipelineExecutor.execute(
            listOf(GoTask("a"), FailTask("b", setOf("a")), GoTask("c", setOf("b"))),
            PrintMonitor()).results
        assertEquals(2, results.size)
        assertEquals("go", results["a"]?.value)
        assertNotNull(results["b"]?.error)
        assertNull(results["c"]?.value)
    }

    class GoTask(id: String, deps: Set<String> = setOf()): AiTask<String>(id, null, deps) {
        override suspend fun execute(inputs: Map<String, AiTaskResult<*>>, monitor: AiTaskMonitor) =
            AiTaskResult.result("go", modelId = null)
    }

    class FailTask(id: String, deps: Set<String> = setOf()): AiTask<String>(id, null, deps) {
        override suspend fun execute(inputs: Map<String, AiTaskResult<*>>, monitor: AiTaskMonitor) =
            AiTaskResult.error("fail", Exception("fail"))
    }

}