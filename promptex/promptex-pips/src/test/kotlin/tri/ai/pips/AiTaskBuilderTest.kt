/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tri.ai.core.tool.ExecContext

class AiTaskBuilderTest {

    private fun printingExecContext() = ExecContext(monitor = PrintMonitor())

    @Test
    fun testExecute() {
        runTest {
            val plan = AiTaskBuilder.task("first") {
                "go"
            }.task("second") { input, _ ->
                input + input
            }.plan
            val result = AiWorkflowExecutor.execute(plan, printingExecContext())
            assertEquals("gogo", result.finalResult.firstValue.textContent())
        }
    }

    @Test
    fun testExecuteList() {
        val plan = AiTaskBuilder.task("first") {
            listOf("go", "stop")
        }.taskOnEach("second") { it, _ ->
            it + it
        }.plan
        val result = runBlocking {
            AiWorkflowExecutor.execute(plan, printingExecContext())
        }
        assertEquals(listOf("gogo", "stopstop"), result.finalResult.firstValue.content())

        val plan2 = AiTaskBuilder.task("first") {
            listOf("go", "stop")
        }.task("second") { it, _ ->
            it.joinToString()
        }.plan
        val result2 = runBlocking {
            AiWorkflowExecutor.execute(plan2, printingExecContext())
        }
        assertEquals("go, stop", result2.finalResult.firstValue.textContent())
    }

}
