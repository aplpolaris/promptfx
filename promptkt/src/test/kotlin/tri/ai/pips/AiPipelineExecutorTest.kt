/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
        assertEquals("go", results["pass"]?.values!![0])
        assertNotNull(results["fail"]?.error)
    }

    @Test
    fun testExecuteChain() = runTest {
        val results = AiPipelineExecutor.execute(
            listOf(GoTask("a"), GoTask("b", setOf("a")), GoTask("c", setOf("b"))),
            PrintMonitor()).results
        assertEquals(3, results.size)
        assertEquals("go", results["a"]?.values!![0])
        assertEquals("go", results["b"]?.values!![0])
        assertEquals("go", results["c"]?.values!![0])
    }

    @Test
    fun testExecuteChainWithFailure() = runTest {
        val results = AiPipelineExecutor.execute(
            listOf(GoTask("a"), FailTask("b", setOf("a")), GoTask("c", setOf("b"))),
            PrintMonitor()).results
        assertEquals(2, results.size)
        assertEquals("go", results["a"]?.values!![0])
        assertNotNull(results["b"]?.error)
        assertNull(results["c"]?.values!![0])
    }

    class GoTask(id: String, deps: Set<String> = setOf()): AiTask<String>(id, null, deps) {
        override suspend fun execute(inputs: Map<String, AiTaskResult<*>>, monitor: AiTaskMonitor) =
            AiTaskResult.result("go", modelId = null)
    }

    class FailTask(id: String, deps: Set<String> = setOf()): AiTask<String>(id, null, deps) {
        override suspend fun execute(inputs: Map<String, AiTaskResult<*>>, monitor: AiTaskMonitor) =
            AiTaskResult.error<String>("fail", Exception("fail"))
    }

}
