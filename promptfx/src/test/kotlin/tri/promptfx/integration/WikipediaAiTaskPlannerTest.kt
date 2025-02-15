/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx.integration

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
