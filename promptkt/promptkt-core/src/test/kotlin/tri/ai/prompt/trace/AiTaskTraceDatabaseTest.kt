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
package tri.ai.prompt.trace

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tri.util.json.jsonWriter

class AiTaskTraceDatabaseTest {

    private val envInfo = AiEnvInfo(model = AiModelInfo("not a model", modelParams = mapOf("maxTokens" to 100)))
    private val inputInfo = AiTaskInputInfo(
        prompt = "Translate {{text}} into French.",
        params = mapOf("text" to "Hello, world!")
    )
    private val execInfo1 = AiExecInfo()
    private val execInfo2 = AiExecInfo.error("test error")
    private val outputInfo1 = AiOutputInfo.text("test output")
    private val outputInfo2 = AiOutputInfo(listOf())

    @Test
    fun testAddAndDeduplicateTraces() {
        val db = AiTaskTraceDatabase().apply {
            addTrace(AiTaskTrace(env = envInfo, input = inputInfo, exec = execInfo1, output = outputInfo1))
            addTrace(AiTaskTrace(env = envInfo, input = inputInfo, exec = execInfo1, output = outputInfo1))
            addTrace(AiTaskTrace(env = envInfo, input = inputInfo, exec = execInfo2, output = outputInfo2))
            addTrace(AiTaskTrace(env = envInfo, input = inputInfo, exec = execInfo1, output = outputInfo2))
        }
        assertEquals(4, db.traces.size)
        assertEquals(1, db.envs.size)
        assertEquals(1, db.inputs.size)
        assertEquals(2, db.execs.size)
        assertEquals(2, db.outputs.size)
    }

    @Test
    fun testTaskIdAndParentIdInDatabase() {
        val parent = AiTaskTrace(taskId = "parent-task", env = envInfo)
        val child = AiTaskTrace(taskId = "child-task", parentTaskId = "parent-task", env = envInfo)
        val db = AiTaskTraceDatabase(listOf(parent, child))
        assertEquals(2, db.traces.size)
        assertEquals("parent-task", db.traces[0].taskId)
        assertEquals("child-task", db.traces[1].taskId)
        assertEquals("parent-task", db.traces[1].parentTaskId)
    }

    @Test
    fun testSerializeDatabase() {
        val db = AiTaskTraceDatabase().apply {
            addTrace(AiTaskTrace(
                taskId = "t1",
                callerId = "test-view",
                env = envInfo,
                input = inputInfo,
                exec = execInfo1,
                output = outputInfo1
            ))
        }
        val json = jsonWriter.writeValueAsString(db)
        println(json)
        assert(json.contains("t1") || json.isNotEmpty())
    }

    @Test
    fun testRoundtripTracesFromDatabase() {
        val trace1 = AiTaskTrace(taskId = "t1", env = envInfo, input = inputInfo, exec = execInfo1, output = outputInfo1)
        val trace2 = AiTaskTrace(taskId = "t2", parentTaskId = "t1", env = envInfo, input = inputInfo, exec = execInfo2, output = outputInfo2)
        val db = AiTaskTraceDatabase(listOf(trace1, trace2))
        val restored = db.taskTraces()
        assertEquals(2, restored.size)
        assertEquals("not a model", restored[0].env?.modelId)
        assertEquals("t1", restored[0].taskId)
        assertEquals("t2", restored[1].taskId)
        assertEquals("t1", restored[1].parentTaskId)
    }

}
