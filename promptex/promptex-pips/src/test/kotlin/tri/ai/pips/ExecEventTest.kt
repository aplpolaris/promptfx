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

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.core.tool.ExecContext
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace

class ExecEventTest {

    /** Verify [ExecEvent] hierarchy covers expected subtypes. */
    @Test
    fun testExecEventHierarchy() {
        val task = object : AiTask("test-task") {
            override suspend fun execute(input: Any?, context: ExecContext) =
                AiPromptTrace(outputInfo = AiOutputInfo.text("result"))
        }

        val events: List<ExecEvent> = listOf(
            ExecEvent.TaskStarted(task),
            ExecEvent.TaskUpdate(task, 0.5),
            ExecEvent.TaskCompleted(task, "result"),
            ExecEvent.TaskFailed(task, RuntimeException("error")),
            ExecEvent.User("hello"),
            ExecEvent.Progress("processing..."),
            ExecEvent.Reasoning("thinking..."),
            ExecEvent.PlanningTask("task-id", "description"),
            ExecEvent.UsingTool("tool", "input"),
            ExecEvent.ToolResult("tool", "result"),
            ExecEvent.StreamingToken("token"),
            ExecEvent.Error(RuntimeException("failure"))
        )

        assertEquals(12, events.size)
        assertTrue(events.all { it is ExecEvent })

        // Verify task lifecycle events
        val taskStarted = events.filterIsInstance<ExecEvent.TaskStarted>()
        assertEquals(1, taskStarted.size)
        assertEquals("test-task", taskStarted.first().task.id)

        // Verify agent/chat events
        val progress = events.filterIsInstance<ExecEvent.Progress>()
        assertEquals(1, progress.size)
        assertEquals("processing...", progress.first().message)
    }

    /** Verify [AiTaskMonitor] typealias works as [FlowCollector]<[ExecEvent]>. */
    @Test
    fun testAiTaskMonitorTypealias() {
        // AiTaskMonitor should be a typealias for FlowCollector<ExecEvent>
        val monitor: AiTaskMonitor = IgnoreMonitor
        assertNotNull(monitor)
    }

    /** Verify [IgnoreMonitor] silently discards all events. */
    @Test
    fun testIgnoreMonitor() = runTest {
        val task = object : AiTask("ignore-test") {
            override suspend fun execute(input: Any?, context: ExecContext) =
                AiPromptTrace(outputInfo = AiOutputInfo.text("done"))
        }
        // Should not throw
        IgnoreMonitor.emit(ExecEvent.TaskStarted(task))
        IgnoreMonitor.emit(ExecEvent.Progress("step"))
        IgnoreMonitor.emit(ExecEvent.Error(RuntimeException("err")))
    }

    /** Verify [ExecEvent] extension functions emit the right types. */
    @Test
    fun testEmitExtensionFunctions() = runTest {
        val collected = mutableListOf<ExecEvent>()
        val collector = object : FlowCollector<ExecEvent> {
            override suspend fun emit(value: ExecEvent) { collected.add(value) }
        }

        val task = object : AiTask("ext-test") {
            override suspend fun execute(input: Any?, context: ExecContext) =
                AiPromptTrace(outputInfo = AiOutputInfo.text("done"))
        }

        with(collector) {
            emitTaskStarted(task)
            emitTaskCompleted(task, "result")
            emitProgress("msg")
            emitReasoning("thought")
            emitUsingTool("tool", "input")
            emitToolResult("tool", "output")
            emitStreamingToken("tok")
            emitError(RuntimeException("err"))
        }

        assertEquals(8, collected.size)
        assertInstanceOf(ExecEvent.TaskStarted::class.java, collected[0])
        assertInstanceOf(ExecEvent.TaskCompleted::class.java, collected[1])
        assertInstanceOf(ExecEvent.Progress::class.java, collected[2])
        assertInstanceOf(ExecEvent.Reasoning::class.java, collected[3])
        assertInstanceOf(ExecEvent.UsingTool::class.java, collected[4])
        assertInstanceOf(ExecEvent.ToolResult::class.java, collected[5])
        assertInstanceOf(ExecEvent.StreamingToken::class.java, collected[6])
        assertInstanceOf(ExecEvent.Error::class.java, collected[7])
    }

    /** Verify [AiPipelineExecutor] emits the correct events. */
    @Test
    fun testPipelineExecutorEmitsEvents() = runTest {
        val collected = mutableListOf<ExecEvent>()
        val collector = object : FlowCollector<ExecEvent> {
            override suspend fun emit(value: ExecEvent) { collected.add(value) }
        }

        val tasks = listOf(
            object : AiTask("task-a") {
                override suspend fun execute(input: Any?, context: ExecContext) =
                    AiPromptTrace(outputInfo = AiOutputInfo.text("a"))
            }
        )
        AiPipelineExecutor.execute(tasks, collector)

        val started = collected.filterIsInstance<ExecEvent.TaskStarted>()
        val completed = collected.filterIsInstance<ExecEvent.TaskCompleted>()
        assertEquals(1, started.size)
        assertEquals("task-a", started.first().task.id)
        assertEquals(1, completed.size)
        assertEquals("task-a", completed.first().task.id)
    }
}
