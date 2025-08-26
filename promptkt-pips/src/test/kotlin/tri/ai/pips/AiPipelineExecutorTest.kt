/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.pips

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiPromptTraceSupport

class AiPipelineExecutorTest {

    @Test
    fun testExecute() {
        runTest {
            val results = AiPipelineExecutor.execute(
                listOf(GoTask("pass"), FailTask("fail")),
                PrintMonitor()).interimResults
            assertEquals(2, results.size)
            assertEquals("go", results["pass"]?.firstValue!!.textContent())
            assertNotNull(results["fail"]?.errorMessage)
        }
    }

    @Test
    fun testExecuteChain() {
        runTest {
            val results = AiPipelineExecutor.execute(
                listOf(GoTask("a"), GoTask("b", setOf("a")), GoTask("c", setOf("b"))),
                PrintMonitor()).interimResults
            assertEquals(3, results.size)
            assertEquals("go", results["a"]?.firstValue!!.textContent())
            assertEquals("go", results["b"]?.firstValue!!.textContent())
            assertEquals("go", results["c"]?.firstValue!!.textContent())
        }
    }

    @Test
    fun testExecuteChainWithFailure() {
        runTest {
            val results = AiPipelineExecutor.execute(
                listOf(GoTask("a"), FailTask("b", setOf("a")), GoTask("c", setOf("b"))),
                PrintMonitor()).interimResults
            assertEquals(2, results.size)
            assertEquals("go", results["a"]?.firstValue!!.textContent())
            assertNotNull(results["b"]?.errorMessage)
            assertNull(results["c"]?.values)
        }
    }

    class GoTask(id: String, deps: Set<String> = setOf()): AiTask(id, null, deps) {
        override suspend fun execute(inputs: Map<String, AiPromptTraceSupport>, monitor: AiTaskMonitor) =
            AiPromptTrace(outputInfo = AiOutputInfo.text("go"))
    }

    class FailTask(id: String, deps: Set<String> = setOf()): AiTask(id, null, deps) {
        override suspend fun execute(inputs: Map<String, AiPromptTraceSupport>, monitor: AiTaskMonitor) =
            AiPromptTrace.error(null, "fail", Exception("fail"))
    }

}
