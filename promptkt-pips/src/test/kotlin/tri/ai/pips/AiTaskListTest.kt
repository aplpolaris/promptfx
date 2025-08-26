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

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tri.ai.prompt.trace.AiOutput
import tri.ai.prompt.trace.AiPromptTrace

class AiTaskListTest {

    @Test
    fun testExecute() {
        runTest {
            val plan = tasktext("first") {
                "go"
            }.aitask("second") {
                val tc = it.textContent()
                AiPromptTrace.output(tc + tc)
            }.planner
            val result = AiPipelineExecutor.execute(plan.plan(), PrintMonitor())
            assertEquals("gogo", result.finalResult.firstValue.text)
        }
    }

    @Test
    fun testExecuteList() {
        val plan = tasklist("first") {
            listOf("go", "stop").map { AiOutput(text = it) }
        }.aitaskonlist("second") {
            AiPromptTrace.outputListAsSingleResult(it)
        }.planner
        val result = runBlocking {
            AiPipelineExecutor.execute(plan.plan(), PrintMonitor())
        }
        assertEquals(listOf("go", "stop"), result.finalResult.firstValue.content())

        val plan2 = aitask("first") {
            AiPromptTrace.output(listOf("go", "stop"))
        }.aitaskonlist("second") {
            AiPromptTrace.output(it.joinToString())
        }.planner
        val result2 = runBlocking {
            AiPipelineExecutor.execute(plan2.plan(), PrintMonitor())
        }
        assertEquals("go, stop", result2.finalResult.firstValue.text)
    }

}
